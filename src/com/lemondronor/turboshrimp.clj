(ns com.lemondronor.turboshrimp
  (:require [com.lemondronor.turboshrimp.at :as at]
            [com.lemondronor.turboshrimp.goals :as goals]
            [com.lemondronor.turboshrimp.navdata :as navdata]
            [taoensso.timbre :as log])
  (:import (java.net DatagramPacket DatagramSocket InetAddress)))


(def default-hostname "192.168.1.1")
(def default-at-port 5556)
(def navdata-port 5554)
(def drones (atom {}))
(def socket-timeout (atom 60000))
(declare mdrone)
(declare drone-init-navdata)


(defn drone-initialize [& options]
  (let [{:keys [name hostname at-port]} options]
    {:name (or name :default)
     :host (InetAddress/getByName
            (or hostname default-hostname))
     :at-port (or at-port default-at-port)
     :counter (atom 0)
     :at-socket (DatagramSocket.)
     :navdata-agent (agent {})
     :navdata-handler (atom nil)
     :navdata-socket (DatagramSocket.)
     :nav-data (atom {})}))


(defn connect [drone]
  (swap! drones assoc (:name drone) drone)
  (mdrone (:name drone) :flat-trim)
  drone)


(defn drone-ip [drone-name]
   (.getHostName ^InetAddress (:host (drone-name @drones))))

(defn send-command [name ^String data]
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
    (send-command name data)))

(defn drone [command-key & [w x y z]]
  (mdrone :default command-key w x y z))

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

(defn- get-ip-from-packet [^DatagramPacket packet]
  (.getHostAddress (.getAddress packet)))

(defn- stream-navdata [agent drone]
  (let [socket (:navdata-socket drone)
        packet nil
        handler nil]
    (log/info "Waiting to receive data")
    (navdata/receive-navdata socket packet)
    (let [ipfrom (get-ip-from-packet packet)
          drone (find-drone ipfrom)
          from-name (first (keys drone))]
      (swap! (get-nav-data from-name)
             (navdata/parse-navdata (navdata/get-navdata-bytes packet)))
      ;;(log/info (str "(" from-name ") " "navdata: "(navdata/log-flight-data (get-nav-data from-name))))
      (communication-check from-name)
      (recur agent drone))))


(defn- start-streaming-navdata [name ^DatagramSocket navdata-socket host port nav-agent]
  (let [receive-data (byte-array 2048)
        nav-datagram-receive-packet (navdata/new-datagram-packet receive-data host port)]
    (log/info "Starting navdata stream")
    (swap! (get-nav-data name) {})
    (.setSoTimeout navdata-socket @socket-timeout)
    (send nav-agent stream-navdata navdata-socket nav-datagram-receive-packet)
    (log/info "Creating navdata stream")))


(declare mdrone-init-navdata)

(defn navdata-error-handler [name]
  (fn [ag ex]
    (println "evil error occured: " ex " and we still have value " @ag)
    (let [^DatagramSocket navdata-socket (:navdata-socket (name @drones))
          nav-agent (:nav-agent (name @drones))]
      (when (= (type ex) java.net.SocketTimeoutException)
        (log/info "Reststarting nav stream")
        (reset! navdata-socket (DatagramSocket. ))
        (log/info "redef navdata-socket")
        (.setSoTimeout navdata-socket @socket-timeout)
        (reset! nav-agent (agent {}))
        (log/info (str "agent now is " nav-agent))
        (mdrone-init-navdata name)))))

(defn- init-streaming-navdata [drone]
  (let [socket (:navdata-socket drone)
        host (:host drone)]
    (let [send-data (byte-array (map byte [1 0 0 0]))
          nav-datagram-send-packet (navdata/new-datagram-packet send-data host navdata-port)]
      (navdata/send-navdata socket nav-datagram-send-packet))))

(defn mdrone-start-navdata [name handler]
  (let [drone (name @drones)
        host (:host drone)
        nav-agent (:nav-agent drone)]
    (log/info "Initializing navdata for" drone)
    (set-error-handler! nav-agent (navdata-error-handler name))
    (init-streaming-navdata drone)
    (mdrone name :init-navdata)
    (mdrone name :control-ack)
    (init-streaming-navdata drone)))

(defn- start-stream [name]
  (let [host (:host (name @drones))
        nav-agent (:nav-agent (name @drones))
        navdata-socket (:navdata-socket (name @drones))]
    (start-streaming-navdata name navdata-socket host navdata-port nav-agent)))

(defn drone-init-navdata []
  (mdrone-init-navdata :default)
  (start-stream :default))
