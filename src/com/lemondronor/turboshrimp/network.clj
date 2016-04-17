(ns ^:no-doc com.lemondronor.turboshrimp.network
  (:import (java.io Closeable)
           (java.net DatagramPacket DatagramSocket InetAddress Socket)
           (java.util Arrays))
  (:gen-class))

(set! *warn-on-reflection* true)


(defn get-addr ^java.net.InetAddress [name]
  (InetAddress/getByName name))


(defn make-datagram-socket ^java.net.DatagramSocket [port]
  (DatagramSocket. (int port)))


(defn make-tcp-socket ^java.net.Socket [^String host ^long port]
  (Socket. host port))


(defn close-socket [^Closeable socket]
  (.close socket))


(defn make-datagram-packet ^java.net.DatagramPacket [size]
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
