(ns com.lemondronor.turboshrimp
  "Control and telemetry library for the AR.Drone."
  (:require [clojure.tools.logging :as log]
            [com.lemondronor.turboshrimp.at :as at]
            [com.lemondronor.turboshrimp.navdata :as navdata])
  (:import (java.io IOException)
           (java.net DatagramPacket DatagramSocket InetAddress))
  (:gen-class))

(set! *warn-on-reflection* true)


;; The default drone hostname.
(def default-hostname "192.168.1.1")

;; The default port to use for AT control commands.
(def default-at-port 5556)

;; The default drone communication timeout, in milliseconds.
(def socket-timeout (atom 60000))


;; This record represents a drone.  The connected?, counter,
;; keep-streaming-navdata and navdata-handler fields are atoms.

(defrecord Drone
    [name
     addr
     hostname
     at-port
     counter
     at-socket
     keep-streaming-navdata
     connected?
     event-handler
     navdata-stream
     navdata])


(defmethod print-method Drone [d ^java.io.Writer w]
  (.write w (str "#<Drone "
                 (:addr d) " "
                 (if @(:connected? d)
                   "[connected]"
                   "[not connected]")
                 ">")))


(defn raise-event [drone event-type & args]
  (log/debug "Event" event-type args)
  (when-let [handler (:event-handler drone)]
    (apply handler event-type args)))


(defn navdata-handler [drone]
  (fn [navdata]
    (reset! (:navdata drone) navdata)
    (raise-event drone :navdata navdata)))


(defn- navdata-error-handler [drone]
  (fn [exception]
    (raise-event drone :error exception)))


(defn make-drone
  "Creates a drone object."
  [& options]
  (let [{:keys [name hostname at-port navdata-port event-handler]} options
        hostname (or hostname default-hostname)
        name (or name :default)
        drone (map->Drone
               {:name name
                :connected? (atom false)
                :event-handler event-handler
                :addr (InetAddress/getByName hostname)
                :hostname hostname
                :at-port (or at-port default-at-port)
                :counter (atom 0)
                :at-socket (DatagramSocket.)
                :navdata-stream (atom nil)
                :navdata (atom {})})]
    (reset! (:navdata-stream drone)
            (navdata/make-navdata-stream
             :hostname hostname
             :port navdata-port
             :navdata-handler (navdata-handler drone)
             :error-handler (navdata-error-handler drone)))
    drone))


(defn send-at-command [drone ^String data]
  (let [^InetAddress addr (:addr drone)
        ^Long at-port (:at-port drone)
        ^DatagramSocket at-socket (:at-socket drone)]
    (log/info "Sending command to" drone data)
    (.send at-socket
           (new DatagramPacket (.getBytes data) (.length data) addr at-port))))

(defn command [drone command-key & [w x y z]]
  (let [counter (:counter drone)
        seq-num (swap! counter inc)
        data (at/build-command command-key seq-num w x y z)]
    (send-at-command drone data)))

(defn connect! [drone]
  (navdata/start-navdata-stream @(:navdata-stream drone))
  (command drone :flat-trim)
  drone)

(defn drone-do-for [drone seconds command-key & [w x y z]]
  (when (pos? seconds)
    (command drone command-key w x y z)
    (Thread/sleep 30)
    (recur drone (- seconds 0.03) command-key [w x y z])))


(defn -main [& args]
  (let [drone (make-drone
               :event-handler (fn [& args]
                                (println args)))]
    (connect! drone)
    (Thread/sleep 1000)))
