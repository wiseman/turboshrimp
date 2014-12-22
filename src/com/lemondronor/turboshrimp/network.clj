(ns com.lemondronor.turboshrimp.network
  (:require [clojure.tools.logging :as log])
  (:import (java.net DatagramPacket DatagramSocket InetAddress)
           (java.util Arrays))
  (:gen-class))

(set! *warn-on-reflection* true)


(defn get-addr ^InetAddress [name]
  (InetAddress/getByName name))


(defn make-datagram-socket ^DatagramSocket [port]
  (DatagramSocket. (int port)))


(defn close-socket [^DatagramSocket socket]
  (.close socket))


(defn make-datagram-packet ^DatagramPacket [size]
  (DatagramPacket. (byte-array size) size))


(defn send-datagram
  [^DatagramSocket socket ^InetAddress host ^long port ^"[B" data-bytes]
  (let [^DatagramPacket packet (DatagramPacket.
                                data-bytes (count data-bytes) host port)]
    (.send socket packet)))


(defn receive-datagram
  ([^DatagramSocket socket ^DatagramPacket packet]
     (.receive socket packet)
     (let [num-bytes (.getLength packet)]
       (log/info "Received" num-bytes "bytes")
       (Arrays/copyOf (.getData packet) num-bytes)))
  ([^DatagramSocket socket]
     (receive-datagram socket (make-datagram-packet 8192))))
