(ns com.lemondronor.turboshrimp.pave
  "Code for reading and parsing video data in Parrot Video
  Encapsulated (PaVE) format."
  (:require [clojure.tools.logging :as log])
  (:import (java.io EOFException InputStream)
           (java.util.concurrent TimeUnit)
           (java.util.concurrent.locks Condition Lock ReentrantLock))
  (:gen-class))



(defn bytes-to-int [^bytes ba offset num-bytes]
  (let [c 0x000000FF]
    (reduce
     #(+ %1 (bit-shift-left (bit-and (aget ba (+ offset %2)) c) (* 8 %2)))
     0
     (range num-bytes))))


(defn get-short [ba offset]
  (bytes-to-int ba offset 2))


(defn get-int [ba offset]
  (bytes-to-int ba offset 4))


(defn get-uint8 [ba offset]
  (bytes-to-int ba offset 1))


(def header-size 76)


(defn full-read
  "Reads a specified number of bytes from an InputStream.

  Compensates for short reads, reading in a loop until the desired
  number of bytes has been read.  Returns a byte array.

  If the first attempt to read from the stream returns 0 bytes, this
  function will return nil.  If subsequent attempts to read from the
  stream return 0 bytes, an EOFException is thrown."
  ^bytes [^InputStream is num-bytes]
  (let [^bytes ba (byte-array num-bytes)]
    (loop [offset 0]
      (let [num-bytes-read (.read is ba offset (- num-bytes offset))]
        (if (<= num-bytes-read 0)
          (if (= offset 0)
            nil
            (throw (EOFException. (str "EOF reading PaVE stream " is))))
          (if (< (+ num-bytes-read offset) num-bytes)
            (recur (+ offset num-bytes-read))
            ba))))))


(def ^"[B" pave-signature (.getBytes "PaVE"))


(defn pave-frame?
  "Checks whether a frame has the 'PaVE' signature."
  [^bytes ba]
  (and (= (aget ba 0) (aget pave-signature 0))
       (= (aget ba 1) (aget pave-signature 1))
       (= (aget ba 2) (aget pave-signature 2))
       (= (aget ba 3) (aget pave-signature 3))))


(defn read-frame
  "Reads a PaVE frame from an InputStream.

  Skips over non-PaVE frames and returns the next PaVE frame.  Returns
  nil if there are no more frames."
  [^InputStream is]
  (if-let [ba (full-read is header-size)]
    (let [this-header-size (get-short ba 6)
          payload-size (get-int ba 8)]
      (assert (>= this-header-size header-size))
      (when (> this-header-size header-size)
        ;; Header size can change from version to version.  Read the
        ;; rest of the header and ignore it if there's more.
        (full-read is (- this-header-size header-size)))
      (if (pave-frame? ba)
        {:header-size header-size
         :payload (or (full-read is payload-size)
                      (throw
                       (EOFException.
                        (str "EOF while reading payload from " is))))
         :display-dimensions [(get-short ba 16) (get-short ba 18)]
         :encoded-dimensions [(get-short ba 12) (get-short ba 14)]
         :frame-number (get-int ba 20)
         :timestamp (get-int ba 24)
         :frame-type (get-uint8 ba 30)
         :slice-index (get-uint8 ba 43)}
        (do
          (log/info "Skipping non-PaVE frame")
          (recur is))))
    nil))


(def IDR-FRAME 1)
(def I-FRAME 2)
(def P-FRAME 3)


(defn i-frame?
  "Checks whether a frame is an I-frame (keyframe)."
  [frame]
  (let [frame-type (:frame-type frame)]
    (or (= frame-type I-FRAME)
        ;; IDR-frames are also I-frames.
        (and (= frame-type IDR-FRAME)
             (= (:slice-index frame) 0)))))


(defrecord LatencyReductionQueue [queue num-frames-dropped lock not-empty])


(defn make-latency-reduction-queue
  "A latency reduction queue implements the recommended latency
  reduction technique for AR.Drone video, which is to drop any
  unprocessed P-frames whenever an I-frame is received."
  []
  (let [^Lock lock (ReentrantLock.)]
    (map->LatencyReductionQueue
     {:queue (atom '())
      :num-dropped-frames (atom 0)
      :lock lock
      :not-empty (.newCondition lock)})))


(defn pull-frame
  "Pulls a video frame from a latency reduction queue.

  Blocks until a frame is available.  An optional timeout (in
  milliseconds) can be specified, in which case the call will return
  nil if a frame isn't available in time."
  [lrq & [timeout-ms]]
  (let [q (:queue lrq)
        ^Lock lock (:lock lrq)
        ^Condition not-empty (:not-empty lrq)]
    (.lock lock)
    (try
      (loop [num-tries 0]
        (if-let [frames (seq @q)]
          (let [[f & new-q] frames]
            (reset! q new-q)
            f)
          (if (> num-tries 0)
            nil
            (do
              (if timeout-ms
                (.await not-empty timeout-ms TimeUnit/MILLISECONDS)
                (.await not-empty))
              (recur (inc num-tries))))))
      (finally
        (.unlock lock)))))


(defn queue-frame
  "Pushes a video frame into a latency reduction queue."
  [lrq frame]
  (let [q (:queue lrq)
        ^Lock lock (:lock lrq)
        ^Condition not-empty (:not-empty lrq)]
    (.lock lock)
    (try
      (do
        (if (i-frame? frame)
          (let [num-skipped (count @q)]
            (when (> num-skipped 0)
              (log/debug "Skipped" num-skipped "frames")
              (swap! (:num-dropped-frames lrq) + num-skipped))
            (reset! q (list frame)))
          (swap! q concat (list frame)))
        (.signal not-empty))
      (finally
        (.unlock lock)))))
