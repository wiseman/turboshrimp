(ns com.lemondronor.turboshrimp.video2
  (:require [clojure.tools.logging :as log]
            [com.lemondronor.turboshrimp.decode :as decode]
            [com.lemondronor.turboshrimp.pave :as pave]
            [com.lemonodor.xio :as xio])
  (:import (java.awt Graphics)
           (javax.swing JFrame JPanel))
  (:gen-class))



(defn display-frame [^JPanel view frame]
  (let [^BufferedImage img (decode/convert-frame (byte-array (map byte (:payload frame))))]
    ;;(.drawImage (.getGraphics view) img 0 0 view)
    ))


(defn render-queue [view rendering? queue]
  (let [frame (atom nil)]
    (dosync
     (alter queue (fn [q]
                    (let [[f new-q] (pave/pop-frame q)]
                      (reset! frame f)
                      new-q))))
    (if-let [f @frame]
      (do
        (let [start (System/currentTimeMillis)]
          (display-frame view f)
          ;;(println (:frame-count f) (:frame-type f) (- (System/currentTimeMillis) start))
          (recur view rendering? queue)))
      (do
        (log/info "Stopping rendering")
        (dosync
         (ref-set rendering? false))))))


(defn view-video [video-byte-seq]
  (let [^JFrame window (JFrame. "Drone video")
        ^JPanel view (JPanel.)
        rendering? (ref false)
        frame-queue (ref '())
        frame-count (atom 0)]
    (.setBounds window 0 0 640 360)
    (.add (.getContentPane window) view)
    (.setVisible window true)
    (let [^Graphics g (.getGraphics view)]
      (doseq [frame (pave/pave-packets video-byte-seq)]
        (let [frame (assoc frame :frame-count @frame-count)]
          (swap! frame-count inc)
          (dosync
           (alter frame-queue pave/add-frame frame)
           (when (not @rendering?)
             (ref-set rendering? true)
             (.start (Thread. (fn [] (render-queue view rendering? frame-queue)))))))))))

(defn decode-video [video-byte-seq]
  (let [start (System/currentTimeMillis)
        frames (pave/pave-packets video-byte-seq)]
    (doseq [frame frames]
      (decode/convert-frame (byte-array (map byte (:payload frame)))))
    (let [end (System/currentTimeMillis)
          dur (- end start)]
      (println "Decoded" (count frames) "frames in" dur "ms"
               (str "(" (/ (count frames) (/ dur 1000.0)) " fps)")))))


(defn -main [& args]
  (decode-video (xio/binary-slurp (first args))))

;;(defn -main [& args]
;;  (view-video (pave/tcp-byte-sequence "192.168.1.1" 5555)))
