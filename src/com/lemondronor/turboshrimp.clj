(ns com.lemondronor.turboshrimp
  (:require [com.lemondronor.turboshrimp.at :as at]
            [com.lemondronor.turboshrimp.goals :as goals]
            [com.lemondronor.turboshrimp.navdata :as navdata]
            [taoensso.timbre :as log])
  (:import (java.net DatagramPacket DatagramSocket InetAddress)))


(def default-drone-ip "192.168.1.1")
(def default-at-port 5556)
(def navdata-port 5554)
(def drones (atom {}))
(def socket-timeout (atom 60000))
(declare mdrone)
(declare drone-init-navdata)


(defn drone-initialize
  ([] (drone-initialize :default default-drone-ip default-at-port))
  ([name ip at-port]
     (swap! drones assoc name {:host (InetAddress/getByName ip)
                               :at-port at-port
                               :counter (atom 0)
                               :at-socket (DatagramSocket. )
                               :nav-agent (agent {})
                               :navdata-socket (DatagramSocket. )
                               :nav-data (atom {})
                               :current-goal-list (atom [])
                               :current-goal (atom "None")
                               :current-belief (atom [])})
     (mdrone name :flat-trim)))

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

(defn drone-stop-navdata []
  (reset! navdata/stop-navstream true))

(defn get-nav-data [name]
  (:nav-data (name @drones)))

(defn communication-check [name]
  (when (= :problem (:com-watchdog @(get-nav-data name)))
    (log/info "Watchdog Reset")
    (mdrone name :reset-watchdog)))

(defn get-ip-from-packet [^DatagramPacket packet]
  (.getHostAddress (.getAddress packet)))

(defn stream-navdata [_ socket packet]
  (do
    (log/info "Waiting to receive data")
    (navdata/receive-navdata socket packet)
    (let [ipfrom (get-ip-from-packet packet)
          drone (find-drone ipfrom)
          from-name (first (keys drone))]
      (swap! (get-nav-data from-name)
             (navdata/parse-navdata (navdata/get-navdata-bytes packet)))
      (log/info (str "(" from-name ") " "navdata: "(navdata/log-flight-data (get-nav-data from-name))))
      (communication-check from-name)
      (goals/eval-current-goals drones from-name @(get-nav-data from-name))
      (log/info (goals/log-goal-info drones from-name)))
    (if @navdata/stop-navstream
      (log/info "navstream-ended")
      (recur nil socket packet))))


(defn start-streaming-navdata [name ^DatagramSocket navdata-socket host port nav-agent]
  (let [receive-data (byte-array 2048)
        nav-datagram-receive-packet (navdata/new-datagram-packet receive-data host port)]
    (log/info "Starting navdata stream")
    (swap! (get-nav-data name) {})
    (.setSoTimeout navdata-socket @socket-timeout)
    (send nav-agent stream-navdata navdata-socket nav-datagram-receive-packet)
    (log/info "Creating navdata stream")))


(defn init-streaming-navdata [navdata-socket host port]
  (let [send-data (byte-array (map byte [1 0 0 0]))
        nav-datagram-send-packet (navdata/new-datagram-packet send-data host port)]
    (navdata/reset-navstream)
    (navdata/send-navdata navdata-socket nav-datagram-send-packet)))

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
        (when-not  @navdata/stop-navstream
          (mdrone-init-navdata name))))))

(defn mdrone-init-navdata [name]
  (let [host (:host (name @drones))
        nav-data (:nav-data (name @drones))
        nav-agent (:nav-agent (name @drones))
        navdata-socket (:navdata-socket (name @drones))]
    (log/info "Initializing navdata")
    (log/info nav-agent)
    (reset! nav-data {})
    (set-error-handler! nav-agent (navdata-error-handler name))
    (init-streaming-navdata navdata-socket host navdata-port)
    (mdrone name :init-navdata)
    (mdrone name :control-ack)
    (init-streaming-navdata navdata-socket host navdata-port)))

(defn start-stream [name]
  (let [host (:host (name @drones))
        nav-agent (:nav-agent (name @drones))
        navdata-socket (:navdata-socket (name @drones))]
    (start-streaming-navdata name navdata-socket host navdata-port nav-agent)))

(defn drone-init-navdata []
  (mdrone-init-navdata :default)
  (start-stream :default))
