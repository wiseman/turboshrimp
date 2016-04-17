(ns com.lemondronor.turboshrimp
  "Control and telemetry library for the AR.Drone 2.0.

  By John Wiseman / jjwiseman@gmail.com
  / [lemonodor](http://twitter.com/lemonodor)"
  (:refer-clojure :exclude [ref])
  (:require [clojure.tools.logging :as log]
            [com.lemondronor.turboshrimp.at :as at]
            [com.lemondronor.turboshrimp.navdata :as navdata]
            [com.lemondronor.turboshrimp.network :as network]
            [com.lemondronor.turboshrimp.util :as util])
  (:import (java.io Writer)
           (java.net Socket))
  (:gen-class))

(set! *warn-on-reflection* true)


(def default-hostname
  "The default drone hostname."
  "192.168.1.1")

(def default-at-port
  "The default network port to use for sending AT control commands to
  a drone."
  5556)

(def default-video-port
  "The default network port for streaming video from a drone."
  5555)

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

(alter-meta! (var ->Drone) #(assoc % :private true))
(alter-meta! (var map->Drone) #(assoc % :private true))


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


;; Queues an already built AT command for the drone.
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
  "Creates a drone object.

  This function can optionally take the following keyword arguments:

  `:hostname`
  : The drone's hostname/IP address.

  `:event-handler`
  : A drone event handler function. An event handler should be a
  function of two arguments: `event-type` and `event-data`. There are
  currently two event types defined. `:navdata` events occurr whenever
  the drone sends navdata telemetry (typically many times per second)
  and the data sent with the event is the navdata object. `:error`
  events occur when an exception is thrown on the background thread
  that communicated with the drone. The data sent with the error event
  is the exception that occurred.

  `:name`
  : A readable name for the drone.

  `:at-port`
  : The network port to use for the AT command channel.

  `:navdata-port`
  : The network port to use for navdata telemetry.

  Examples:
  ```
  (def my-drone (make-drone :event-handler #(println %1 %2)))
  ```"
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


(defmacro ^:private def-all-at-commands []
  (let [args-sym (gensym)]
    `(do
       ~@(map (fn [cmd]
                (let [fn-name (symbol (name (:name cmd)))]
                  `(do
                     (defn ~fn-name
                       ~(:doc cmd)
                       [drone# & ~args-sym]
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


(defmacro ^:private defpcmds [[a doc] [b doc]]
  (let [a-key (keyword a)
        b-key (keyword b)]
    `(do
       (defn ~a
         ~doc
         [~'drone ~'speed]
         (swap! (:pcmd ~'drone) assoc-exclusive ~a-key ~b-key ~'speed)
         ~'drone)
       (defn ~b
         ~doc
         [~'drone ~'speed]
         (swap! (:pcmd ~'drone) assoc-exclusive ~b-key ~a-key ~'speed)
         ~'drone))))


(defpcmds
  [up
   "Commands the drone to begin climbing. `speed` should
   be a number in the range [0.0, 1.0]."]
  [down
   "Commands the drone to begin descending. `speed` should
   be a number in the range [0.0, 1.0]."])

(defpcmds
  [left
   "Commands the drone to begin moving (translating) to its left.
   `speed` should be a number in the range [0.0, 1.0]."]
  [right
   "Commands the drone to begin moving (translating) to its right.
   `speed` should be a number in the range [0.0, 1.0]."])

(defpcmds
  [front
   "Commands the drone to begin moving (translating) forward.
   `speed` should be a number in the range [0.0, 1.0]."]
  [back
   "Commands the drone to begin moving (translating) backward.
   `speed` should be a number in the range [0.0, 1.0]."])

(defpcmds
  [clockwise
   "Commands the drone to begin rotating clockwise.
   `speed` should be a number in the range [0.0, 1.0]."]
  [counter-clockwise
    "Commands the drone to begin rotating counter-clockwise.
   `speed` should be a number in the range [0.0, 1.0]."])


(defn video-input-stream
  "Creates an `InputStream` connected to a drone's video stream.

  See [[com.lemondronor.turboshrimp.pave]] for more information about
  how to use a drone's video stream."
  [drone]
  (.getInputStream (Socket. ^String (:hostname drone) ^int default-video-port)))


(defn connect!
  "Connects to a drone's command channel and telemetry channel.

  You must connect to a drone before you can send it any commands or
  receive telemetry."
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
