(ns com.lemondronor.turboshrimp
  "Control and telemetry library for the AR.Drone."
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
    (reset! (:navdata drone) navdata)
    (reset! (:connected? drone) true)
    (raise-event drone :navdata navdata)))


(defn- navdata-error-handler [drone]
  (fn [exception]
    (raise-event drone :error exception)))


(defn- queue-command [drone command]
  (dosync
   (alter (:command-queue drone) concat (list command))))


(defn- pop-commands [drone]
  (dosync
   (let [commands @(:command-queue drone)
         seq-num @(:seq-num drone)]
     (ref-set (:command-queue drone) '())
     (ref-set (:seq-num drone) (+ seq-num (count commands)))
     [seq-num commands])))


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
                :command-queue (ref '())
                :ref (atom {})
                :pcmd (atom {})
                :seq-num (ref 1)
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


(defn takeoff [drone]
  (swap! (:ref drone) assoc :fly true)
  drone)


(defn land [drone]
  (swap! (:ref drone) assoc :fly false)
  drone)


(defn stop [drone]
  (reset! (:pcmd drone) {})
  drone)


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


(defn connect! [drone]
  (reset! (:socket drone) (network/make-datagram-socket (:port drone)))
  (navdata/start-navdata-stream @(:navdata-stream drone))
  (command drone :ctrl 5 0)
  (command drone :navdata-demo true)
  (let [thread-pool (util/make-sched-thread-pool 1)]
    (reset! (:thread-pool drone) thread-pool)
    (reset! (:command-executor drone)
            (util/periodic-task
             30
             (fn []
               (try
                 (command drone :ref @(:ref drone))
                 (command drone :pcmd @(:pcmd drone))
                 (send-commands drone)
                 (catch Throwable e
                   (log/error e "Error sending commands"))))
             thread-pool)))
  drone)


(defn disconnect! [drone]
  (util/cancel-scheduled-task @(:command-executor drone))
  (util/shutdown-pool @(:thread-pool drone))
  (network/close-socket @(:socket drone))
  drone)
