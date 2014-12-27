(ns com.lemondronor.turboshrimp.video2
  (:require [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [com.lemondronor.turboshrimp.decode :as decode]
            [com.lemondronor.turboshrimp.pave :as pave]
            [com.lemonodor.xio :as xio])
  (:import (java.awt Graphics)
           (javax.swing JFrame JPanel))
  (:gen-class))



(defn display-frame [^JPanel view frame]
  (let [^BufferedImage img (decode/convert-frame frame)]
    (.drawImage (.getGraphics view) img 0 0 view)))


(defn -main [& args]
  (let [is (io/input-stream (first args))
        ^JFrame window (JFrame. "Drone video")
        ^JPanel view (JPanel.)
        keep-decoding? (atom true)
        lrq (pave/make-latency-reduction-queue)]
    (.setBounds window 0 0 640 360)
    (.add (.getContentPane window) view)
    (.setVisible window true)
    (.start
     (Thread.
      (fn []
        (if @keep-decoding?
          (let [frame (pave/pull-frame lrq 500)]
            (println frame)
            (when frame
              (display-frame view (:payload frame)))
            (recur))
          (log/info "exiting")))))
    (loop [frame (pave/read-frame is)]
      (if frame
        (do
          (pave/queue-frame lrq frame)
          (Thread/sleep 35)
          (recur (pave/read-frame is)))
        (do
          (reset! keep-decoding? false)
          (Thread/sleep 100)
          (println lrq))))))
