(ns com.lemondronor.turboshrimp
  "Control and telemetry library for the AR.Drone."
  (:refer-clojure :exclude [ref])
  (:require [clojure.tools.logging :as log]
            [com.lemondronor.turboshrimp.at :as at]
            [com.lemondronor.turboshrimp.navdata :as navdata]
            [com.lemondronor.turboshrimp.network :as network]
            [com.lemondronor.turboshrimp.util :as util])
  (:import (java.io Writer))
  (:gen-class))

(set! *warn-on-reflection* true)


;; The default drone hostname.
(def default-hostname "192.168.1.1")

;; The default port to use for AT control commands.
(def default-at-port 5556)

;; The default port for streaming video.
(def default-video-port 5555)

;; The default drone communication timeout, in milliseconds.
(def socket-timeout (atom 60000))


;; This record represents a drone.  The connected?, counter,
;; keep-streaming-navdata and navdata-handler fields are atoms.

(defrecord ^:private Drone
    [name
     hostname
     socket
     port
     addr
     command-queue
     ref
     pcmd
     disable-emergency?
     seq-num
     connected?
     event-handler
     navdata-stream
     navdata
     thread-pool
     command-executor])


(defmethod print-method Drone [d ^Writer w]
  (.write w (str "#<Drone "
                 (:addr d) " "
                 (if @(:connected? d)
                   "[connected]"
                   "[not connected]")
                 ">")))


(defn- raise-event [drone event-type & args]
  (log/debug "Event" event-type args)
  (when-let [handler (:event-handler drone)]
    (apply handler event-type args)))


(defn- navdata-handler [drone]
  (fn [navdata]
    ;; Update the drone's current navdata.
    (reset! (:navdata drone) navdata)
    ;; Receipt of navdata tells us we're connected to the drone.
    (reset! (:connected? drone) true)
    ;; If a user has requested a reset of the emergency state, do that
    ;; now.
    (let [disable-emergency? (:disable-emergency? drone)
          state (:state navdata)]
      (if (and state (= (:emergency state) :detected) @disable-emergency?)
        (swap! (:ref drone) assoc :emergency true)
        (do
          (swap! (:ref drone) assoc :emergency false)
          (reset! disable-emergency? false))))
    (raise-event drone :navdata navdata)))


(defn- navdata-error-handler [drone]
  (fn [exception]
    (raise-event drone :error exception)))


;; Queue's an already built AT command for the drone.
(defn- queue-command [drone command]
  (dosync
   (alter (:command-queue drone) concat (list command))))


;; Pops all commands off the command queue and updates the command
;; sequence number.
(defn- pop-commands [drone]
  (dosync
   (let [commands @(:command-queue drone)
         seq-num @(:seq-num drone)]
     (ref-set (:command-queue drone) '())
     (ref-set (:seq-num drone) (+ seq-num (count commands)))
     [seq-num commands])))


;; Sends queued commands to the drone.
(defn- send-commands [drone]
  (let [[seq-num commands] (pop-commands drone)]
    (when (seq commands)
      (log/debug "Sending" commands)
      (network/send-datagram
       @(:socket drone)
       (:addr drone)
       (:port drone)
       (at/commands-bytes seq-num commands)))))


(defn make-drone
  "Creates a drone object."
  [& options]
  (let [{:keys [name hostname at-port navdata-port event-handler]} options
        hostname (or hostname default-hostname)
        at-port (or at-port default-at-port)
        name (or name :default)
        drone (map->Drone
               {:name name
                :hostname hostname
                :socket (atom nil)
                :addr (network/get-addr hostname)
                :port at-port
                :command-queue (clojure.core/ref '())
                :ref (atom {})
                :pcmd (atom {})
                :disable-emergency? (atom false)
                :seq-num (clojure.core/ref 1)
                :connected? (atom false)
                :event-handler event-handler
                :navdata-stream (atom nil)
                :navdata (atom {})
                :thread-pool (atom nil)
                :command-executor (atom nil)})]
    (reset! (:navdata-stream drone)
            (navdata/make-navdata-stream
             :hostname hostname
             :port navdata-port
             :navdata-handler (navdata-handler drone)
             :error-handler (navdata-error-handler drone)))
    drone))


;; Builds and queues an AT command for the drone.
(defn- command [drone command-key & args]
  (queue-command drone (apply at/build-command command-key args))
  drone)


(defmacro def-all-at-commands []
  (let [args-sym (gensym)]
    `(do
       ~@(map (fn [cmd]
                (let [fn-name (symbol (name (:name cmd)))]
                  `(do
                     (defn ~fn-name [drone# & ~args-sym]
                       (apply command drone# ~(:name cmd) ~args-sym))
                     (alter-meta!
                      (var ~fn-name)
                      (fn [metadata#]
                        (assoc metadata# :arglists (list '~(:args cmd))))))))
              (vals @at/command-table)))))


(def-all-at-commands)


(defn takeoff
  "Commands the drone to take off."
  [drone]
  (swap! (:ref drone) assoc :fly true)
  drone)


(defn land
  "Commands the drone to land."
  [drone]
  (swap! (:ref drone) assoc :fly false)
  drone)


(defn stop
  "Commands the drone to stop all motion."
    [drone]
  (reset! (:pcmd drone) {})
  drone)


(defn disable-emergency
  "Resets the drone's emergency stop state."
  [drone]
  (reset! (:disable-emergency? drone) true))


(defn- assoc-exclusive
  "Sets k1 to v and removes k2 from a map."
  [map k1 k2 v]
  (-> map
      (assoc k1 v)
      (dissoc k2)))


(defmacro ^:private defpcmds [[a b]]
  (let [a-key (keyword a)
        b-key (keyword b)]
    `(do
       (defn ~a [~'drone ~'speed]
         (swap! (:pcmd ~'drone) assoc-exclusive ~a-key ~b-key ~'speed)
         ~'drone)
       (defn ~b [~'drone ~'speed]
         (swap! (:pcmd ~'drone) assoc-exclusive ~b-key ~a-key ~'speed)
         ~'drone))))


(defpcmds [up down])
(defpcmds [left right])
(defpcmds [front back])
(defpcmds [clockwise counter-clockwise])


(defn connect!
  "Connects to a drone's command channel and telemtry channel."
  [drone]
  (reset! (:socket drone) (network/make-datagram-socket (:port drone)))
  (navdata/start-navdata-stream @(:navdata-stream drone))
  (ctrl drone 5 0)
  (navdata-demo drone true)
  (let [thread-pool (util/make-sched-thread-pool 1)]
    (reset! (:thread-pool drone) thread-pool)
    (reset! (:command-executor drone)
            (util/periodic-task
             30
             (fn []
               (try
                 (ref drone @(:ref drone))
                 (pcmd drone @(:pcmd drone))
                 (send-commands drone)
                 (catch Throwable e
                   (log/error e "Error sending commands"))))
             thread-pool)))
  drone)


(defn disconnect!
  "Disconnects from a drone."
  [drone]
  (when-let [executor @(:command-executor drone)]
    (util/cancel-scheduled-task executor))
  (when-let [pool @(:thread-pool drone)]
    (util/shutdown-pool pool))
  (when-let [socket @(:socket drone)]
    (network/close-socket socket))
  drone)
