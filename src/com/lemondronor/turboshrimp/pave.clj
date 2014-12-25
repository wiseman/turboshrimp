(ns com.lemondronor.turboshrimp.pave
  (:require [clojure.tools.logging :as log]
            [com.lemonodor.xio :as xio])
  (:import (java.net Socket))
  (:gen-class))


(defn bytes-to-int [ba offset num-bytes]
  (let [c 0x000000FF]
    (reduce
     #(+ %1 (bit-shift-left (bit-and (nth ba (+ offset %2)) c) (* 8 %2)))
     0
     (range num-bytes))))


(defn get-short [ba offset]
  (bytes-to-int ba offset 2))


(defn get-int [ba offset]
  (bytes-to-int ba offset 4))


(defn get-uint8 [ba offset]
  (bytes-to-int ba offset 1))


(defn pave-packets [byte-sequence]
  (let [signature (take 4 byte-sequence)
        header-size-bytes (take 2 (drop 6 byte-sequence))
        payload-size-bytes (take 4 (drop 8 byte-sequence))
        frame-type-byte (take 1 (drop 30 byte-sequence))]
    (if (seq frame-type-byte)
      (let [signature (String. (byte-array (map byte signature)))
            header-size (get-short header-size-bytes 0)
            payload-size (get-int payload-size-bytes 0)
            frame-type (get-uint8 frame-type-byte 0)]
        (Thread/sleep 10)
        (cons
         {:signature signature
          :header-size header-size
          :payload-size payload-size
          :frame-type frame-type
          :payload (take payload-size (drop header-size byte-sequence))}
         (lazy-seq (pave-packets
                    (drop (+ header-size payload-size) byte-sequence)))))
      nil)))


(def IDR-FRAME 1)
(def I-FRAME 2)
(def P-FRAME 3)


(defn add-frame [queue frame]
  (if (= (:frame-type frame) IDR-FRAME)
    (let [num-skipped-frames (count queue)]
      (log/info "Skipping" num-skipped-frames)
      (list frame))
    (concat queue (list frame))))


(defn pop-frame [queue]
  (if (seq queue)
    [(first queue) (rest queue)]
    [nil '()]))


(defn tcp-byte-sequence [host port]
  (let [socket (Socket. host port)
        buffer (byte-array 2048)
        is (.getInputStream socket)]
    (letfn ([read-chunks []
             (let [num-read (.read is buffer)]
               (if (> num-read 0)
                 (lazy-cat (subvec (vec (seq buffer)) 0 num-read)
                           (read-chunks))))])
      (read-chunks))))


(defn -main [& args]
  (let [video-data (xio/binary-slurp (first args))]
    (doseq [p (pave-packets video-data)]
      (println p))))


;; (defn -main [& args]
;;   (doseq [p (pave-packets (tcp-byte-sequence "192.168.1.1" 5555))]
;;     (println p)))
