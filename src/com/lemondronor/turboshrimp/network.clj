(ns com.lemondronor.turboshrimp.network
  (:import (java.net DatagramPacket DatagramSocket InetAddress Socket)
           (java.util Arrays))
  (:gen-class))

(set! *warn-on-reflection* true)


(defn get-addr ^InetAddress [name]
  (InetAddress/getByName name))


(defn make-datagram-socket ^DatagramSocket [port]
  (DatagramSocket. (int port)))


(defn make-tcp-socket [host port]
  (Socket. host port))


(defn close-socket [^Socket socket]
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
       (Arrays/copyOf (.getData packet) num-bytes)))
  ([^DatagramSocket socket]
     (receive-datagram socket (make-datagram-packet 8192))))
