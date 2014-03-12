(ns com.lemondronor.turboshrimp
  "Control and telemetry library for the AR.Drone."
  (:require [com.lemondronor.turboshrimp.at :as at]
            [com.lemondronor.turboshrimp.goals :as goals]
            [com.lemondronor.turboshrimp.navdata :as navdata]
            [taoensso.timbre :as log])
  (:import (java.io IOException)
           (java.net DatagramPacket DatagramSocket InetAddress)))

(set! *warn-on-reflection* true)

(def default-hostname "192.168.1.1")
(def default-at-port 5556)
(def navdata-port 5554)
(def drones (atom {}))
(def socket-timeout (atom 60000))


(defrecord Drone
    [name
     host
     at-port
     counter
     at-socket
     keep-streaming-navdata
     navdata-handler
     navdata-socket
     nav-data])


(defn drone-ip [drone]
   (.getHostName ^InetAddress (:host drone)))

(defmethod print-method Drone [d ^java.io.Writer w]
  (.write w (str "#<Drone "
                 (drone-ip d) " "
                 (if (@drones (:name d))
                   "[connected]"
                   "[not connected]")
                 ">")))


(defn make-drone [& options]
  (let [{:keys [name hostname at-port event-handler]} options
        name (or name :default)]
    (map->Drone
     {:name name
      :event-handler event-handler
      :host (InetAddress/getByName
             (or hostname default-hostname))
      :at-port (or at-port default-at-port)
      :counter (atom 0)
      :at-socket (DatagramSocket.)
      :keep-streaming-navdata (atom false)
      :navdata-handler (atom nil)
      :navdata-socket (DatagramSocket.)
      :nav-data (atom {})})))

(defn send-at-command [name ^String data]
  (let [^InetAddress host (:host (name @drones))
        ^Long at-port (:at-port (name @drones))
        ^DatagramSocket at-socket (:at-socket (name @drones))]
    (log/info "Sending command to" name data)
    (.send at-socket
           (new DatagramPacket (.getBytes data) (.length data) host at-port))))

(defn mdrone [name command-key & [w x y z]]
  (let [counter (:counter (name @drones))
        seq-num (swap! counter inc)
        data (at/build-command command-key seq-num w x y z)]
    (send-at-command name data)))

(defn drone [command-key & [w x y z]]
  (mdrone :default command-key w x y z))

(defn- navdata-error-handler [drone]
  (fn [agent exception]
    (log/error exception "Exception in navdata agent for" drone)
    (when-let [event-handler (:event-handler drone)]
      (event-handler :error drone exception))))

(defn send-datagram
  [^DatagramSocket socket ^InetAddress host ^long port ^"[B" data-bytes]
  (let [^DatagramPacket packet (DatagramPacket. data-bytes (count data-bytes) host port)]
    (.send socket packet)))


(defn raise-event [drone event-type & args]
  (when-let [handler (:event-handler drone)]
    (apply handler event-type args)))

(defn process-navdata [drone navdata]
  (raise-event drone :navdata navdata))

(defn- navdata-thread-fn [drone]
  (let [^DatagramSocket socket (:navdata-socket drone)
        ^DatagramPacket packet (navdata/new-datagram-packet
                                (byte-array 2048) (:host drone) navdata-port)]
    (loop []
      (when @(:keep-streaming-navdata drone)
        (try
          (do
            (navdata/receive-navdata socket packet)
            (process-navdata drone (navdata/parse-navdata (.getData packet))))
          (catch IOException e
            (reset! (:keep-streaming-navdata drone) false)
            (raise-event drone :error e)))
        (recur)))))

(defn- start-navdata! [drone]
  (reset! (:keep-streaming-navdata drone) true)
  (doto (Thread.
         (fn [] (navdata-thread-fn drone))
         (str "navdata thread for drone " (:name drone)))
    (.setDaemon true)
    (.start))
  (send-datagram (:navdata-socket drone) (:host drone) navdata-port
                 (byte-array (map byte [1 0 0 0]))))


(defn connect! [drone]
  (swap! drones assoc (:name drone) drone)
  (start-navdata! drone)
  (mdrone (:name drone) :flat-trim)
  drone)

(defn mdrone-do-for [name seconds command-key & [w x y z]]
  (when (pos? seconds)
    (mdrone name command-key w x y z)
    (Thread/sleep 30)
    (recur name (- seconds 0.03) command-key [w x y z])))

(defn drone-do-for [seconds command-key & [w x y z]]
  (mdrone-do-for :default seconds command-key w x y z))

(defn find-drone [ip]
  (select-keys @drones (for [[k v] @drones :when (= ip (.getHostAddress ^InetAddress (:host v)))] k)))

(defn get-nav-data [name]
  (:nav-data (name @drones)))

(defn communication-check [name]
  (when (= :problem (:com-watchdog @(get-nav-data name)))
    (log/info "Watchdog Reset")
    (mdrone name :reset-watchdog)))
