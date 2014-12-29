(ns com.lemondronor.turboshrimp.video2
  (:require [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [com.lemondronor.turboshrimp.h264j :as decode]
            [com.lemondronor.turboshrimp.pave :as pave]
            [com.lemonodor.xio :as xio])
  (:import (java.awt Graphics)
           (javax.swing JFrame JPanel))
  (:gen-class))



(defn display-frame [decoder ^JPanel view frame]
  (let [^BufferedImage img (decoder frame)]
    (when img
      ;(.drawImage (.getGraphics view) img 0 0 view)
      )))


(defn -main [& args]
  (let [is (io/input-stream (first args))
        ^JFrame window (JFrame. "Drone video")
        ^JPanel view (JPanel.)
        keep-decoding? (atom true)
        lrq (pave/make-frame-queue :reduce-latency? false)
        decoder (decode/decoder)]
    (.setBounds window 0 0 640 360)
    (.add (.getContentPane window) view)
    (.setVisible window true)
    (let [start-time (atom nil)
          frame-count (atom 0)]
      (.start
       (Thread.
        (fn []
          (let [frame (pave/pull-frame lrq 100)]
            (if frame
              (do
                (swap! frame-count inc)
                (display-frame decoder view frame)
                (recur))
              (do
                (println (/ @frame-count (/ (- (System/currentTimeMillis) @start-time) 1000.0)) " fps")
                (log/info "exiting")))))))
      (reset! start-time (System/currentTimeMillis))
      (loop [frame (pave/read-frame is)]
        (if frame
          (do
            (pave/queue-frame lrq frame)
            (recur (pave/read-frame is)))
          (do
            ;;(reset! keep-decoding? false)
            (Thread/sleep 100000)
            ;(println lrq)
            ))))))
