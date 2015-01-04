(ns controller
  (:require [clojure.java.io :as io]
            [com.lemondronor.turboshrimp :as ar-drone]
            [com.lemondronor.turboshrimp.pave :as pave]
            [com.lemondronor.turboshrimp.xuggler :as video]
            [com.lemonodor.gflags :as gflags]
            [seesaw.core :as seesaw])
  (:import [java.awt.event InputEvent KeyEvent]))


(defn data-display [text id]
  (seesaw/grid-panel
   :columns 2
   :items [(seesaw/make-widget [:fill-h 10])
           (seesaw/label :text text)
           (seesaw/label :id id :text "0")]))


(defn glue-button [id text]
  (seesaw/horizontal-panel
   :items
   [(seesaw/make-widget :fill-h)
    (seesaw/button :id id :text text)
    (seesaw/make-widget :fill-h)]))


(defn make-ui []
  (seesaw/frame
   :title "Turboshrimp Controller"
   :size [1280 :by 720]
   :content
   (seesaw/border-panel
    :id :video
    :border 0 :hgap 0 :vgap 0
    :size [1280 :by 720])))


(def default-speed 0.5)


(def key-actions
  [[{:code KeyEvent/VK_A :mod InputEvent/SHIFT_MASK} ar-drone/left]
   [{:code KeyEvent/VK_D :mod InputEvent/SHIFT_MASK} ar-drone/right]
   [{:code KeyEvent/VK_W} ar-drone/front]
   [{:code KeyEvent/VK_S} ar-drone/back]
   [{:code KeyEvent/VK_A} ar-drone/counter-clockwise]
   [{:code KeyEvent/VK_D} ar-drone/clockwise]
   [{:code KeyEvent/VK_Q} ar-drone/up]
   [{:code KeyEvent/VK_Z} ar-drone/down]])


(defn key-descriptor-matches? [descriptor ^KeyEvent evt]
  (and (= (:code descriptor) (.getKeyCode evt))
       (if-let [modifier (:mod descriptor)]
         (not (zero? (bit-and modifier (.getModifiers evt))))
         true)))


(defn find-key-action [evt actions]
  (if-let [e (some #(if (key-descriptor-matches? (first %) evt) %) actions)]
    e
    nil))


(defn make-key-controller [ui drone]
  (seesaw/listen
   ui
   :key (fn [^KeyEvent e]
          (let [id (.getID e)]
            (cond
              (= id KeyEvent/KEY_PRESSED)
              (if-let [action (find-key-action e key-actions)]
                (println action))
              (= id KeyEvent/KEY_RELEASED)
              (if-let [action (find-key-action e key-actions)]
                (println "STOPPING" action)))))))


(defn make-controller [ui drone]
  (make-key-controller ui drone))


(defn -main [& args]
  (let [ui (make-ui)
        fq (pave/make-frame-queue)
        is (io/input-stream (first args))
        decoder (video/decoder)
        drone (ar-drone/make-drone)]
    (-> ui seesaw/pack! seesaw/show!)
    (let [view (seesaw/select ui [:#video])]
      (make-controller ui drone)
      (doto
          (Thread.
           (fn []
             (loop []
               (let [image (-> fq
                               pave/pull-frame
                               decoder)]
                 (seesaw/invoke-now
                  (.drawImage (.getGraphics view) image 0 0 1280 720 view)))
               (recur))))
        (.start))
      (loop [frame (pave/read-frame is)]
        (when frame
          (pave/queue-frame fq frame)
          (Thread/sleep 30)
          (recur (pave/read-frame is)))))))
