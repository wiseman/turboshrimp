(ns com.lemondronor.turboshrimp.xvideo
  (:require [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [com.lemondronor.turboshrimp.pave :as pave]
            [com.lemonodor.xio :as xio])
  (:import (com.lemondronor.turboshrimp.xuggler ImageListener XugglerDecoder)
           (java.io ByteArrayInputStream)
           (java.awt Graphics)
           (javax.swing JFrame JPanel))
  (:gen-class))


(defn draw-image [^JPanel view img]
  (.drawImage (.getGraphics view) img 0 0 view))


(defn decode-video [is]
  (let [^JFrame window (JFrame. "Drone video")
        ^JPanel view (JPanel.)
        counter (atom 0)
        listener (proxy [ImageListener] []
                   (imageUpdated [img]
                     (swap! counter inc)
                     (draw-image view img)))
        decoder (XugglerDecoder.)
        start (System/currentTimeMillis)]
    (.setBounds window 0 0 640 360)
    (.add (.getContentPane window) view)
    (.setVisible window true)
    (.setImageListener decoder listener)
    (.decode decoder is)
    (let [end (System/currentTimeMillis)
          dur (- end start)]
      (println "Decoded" @counter "frames in" dur "ms"
               (str "(" (/ @counter (/ dur 1000.0)) " fps)")))))


(defn -main [& args]
  (let [is (->> (first args)
                (xio/binary-slurp)
                (pave/pave-packets)
                (map :payload)
                (apply concat)
                (map byte)
                (byte-array)
                (ByteArrayInputStream.))]
  (decode-video is)))
