(ns controller
  (:require [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [com.lemondronor.turboshrimp :as ar-drone]
            [com.lemondronor.turboshrimp.pave :as pave]
            [com.lemondronor.turboshrimp.xuggler :as video]
            [com.lemonodor.gflags :as gflags]
            [seesaw.core :as seesaw])
  (:import [java.awt.event InputEvent KeyEvent]
           [java.awt Color Font Graphics Graphics2D]
           [java.awt.image BufferedImage]
           [java.net Socket]
           [javax.swing JPanel]))



(defn make-ui []
  (seesaw/frame
   :title "Turboshrimp Controller"
   :size [1280 :by 720]
   :content
   (seesaw/border-panel
    :id :video
    :border 0 :hgap 0 :vgap 0
    :size [1280 :by 720])))


;; Keys are WASD/QZ, with Shift to "strafe":
;; W / S  -  Move forward / Move backward
;; A / D  -  Yaw left / Yaw right
;; Q / Z  -  Climb / Descend
;; Shift-A / Shift D  - Move left / Move right.

(def key-actions
  ;; Checked in order as they appear here, so make sure to put keys
  ;; with mods first.
  [[{:code KeyEvent/VK_A :mod InputEvent/SHIFT_MASK} ar-drone/left]
   [{:code KeyEvent/VK_D :mod InputEvent/SHIFT_MASK} ar-drone/right]
   [{:code KeyEvent/VK_W} ar-drone/front]
   [{:code KeyEvent/VK_S} ar-drone/back]
   [{:code KeyEvent/VK_A} ar-drone/counter-clockwise]
   [{:code KeyEvent/VK_D} ar-drone/clockwise]
   [{:code KeyEvent/VK_Q} ar-drone/up]
   [{:code KeyEvent/VK_Z} ar-drone/down]
   [{:code KeyEvent/VK_T} ar-drone/takeoff :continuous? false]
   [{:code KeyEvent/VK_L} ar-drone/land :continuous? false]
   [{:code KeyEvent/VK_C} #(ar-drone/command % :switch-camera :forward)
    :continuous? false]
   [{:code KeyEvent/VK_V} #(ar-drone/command % :switch-camera :down)
    :continuous? false]])


(defn key-descriptor-matches? [descriptor ^KeyEvent evt]
  (and (= (:code descriptor) (.getKeyCode evt))
       (if-let [modifier (:mod descriptor)]
         (not (zero? (bit-and modifier (.getModifiers evt))))
         true)))


(defn find-key-action [evt actions]
  (if-let [e (some #(if (key-descriptor-matches? (first %) evt) %) actions)]
    (rest e)
    nil))


(def default-speed 0.5)


(defn make-key-controller [drone]
  (fn [^KeyEvent e]
    (let [id (.getID e)]
      (when-let [action (find-key-action e key-actions)]
        (let [[action-fn & {:keys [continuous?]
                            :or {continuous? true}}] action]
          (let [action-args (cons
                             drone
                             (cond
                               (and (= id KeyEvent/KEY_PRESSED) continuous?)
                               (list default-speed)
                               (and (= id KeyEvent/KEY_RELEASED) continuous?)
                               (list 0.0)
                               :else
                               '()))]
            (when (or (= id KeyEvent/KEY_PRESSED)
                      (and (= id KeyEvent/KEY_RELEASED) continuous?))
              (println "Running" action-fn action-args)
              (apply action-fn action-args))))))))


(defn norm [v]
  (Math/sqrt (reduce + (map #(* % %) v))))


(defn deg2rad [d]
  (* d (/ Math/PI 180.0)))


(defn draw-hud [^JPanel view ^Graphics2D g drone]
  (let [navdata @(:navdata drone)
        pitch (get-in navdata [:demo :theta])
        roll (get-in navdata [:demo :phi])
        hdg (get-in navdata [:magneto :heading :fusion-unwrapped])
        alt (get-in navdata [:demo :altitude])
        vel (get-in navdata [:demo :velocity])
        spd (norm (vals vel))]
    (.setFont g (Font. "SansSerif" Font/PLAIN 24))
    (.setColor g Color/WHITE)
    (if spd
      (.drawString g (format "SPD %.1f m/s" (/ spd 1000.0)) 10 30)
      (.drawString g "SPD unk" 10 30))
    (if alt
      (.drawString g (format "ALT %.1f m" (/ alt 1000.0)) 10 60)
      (.drawString g "ALT unk" 10 60))
    (if hdg
      (.drawString g (str "HDG " (int hdg)) 10 90)
      (.drawString g "HDG unk" 10 90))
    (when roll
      (let [h (.getHeight view)
            h2 (/ h 2.0)
            ph (+ h2 (* 1.7 h (Math/sin (deg2rad pitch))))]
        (.rotate g (- (deg2rad roll)) (/ (.getWidth view) 2.0) h2)
        (.drawLine g -500 ph (+ (.getWidth view) 500) ph)
        (.rotate g (deg2rad roll) (/ (.getWidth view) 2.0) h2)
        ))))


(def drone-video-port 5555)


(defn draw-image [^JPanel view ^Graphics g ^BufferedImage image]
  (.drawImage g image 0 0 (.getWidth view) (.getHeight view) nil))


(defn connect-video-controller [ui drone]
  (let [is (.getInputStream (Socket. (:hostname drone) drone-video-port))
        fq (pave/make-frame-queue)
        ^JPanel view (seesaw/select ui [:#video])
        decoder (video/decoder)]
    (doto
        (Thread.
         (fn []
           (loop [frame (pave/read-frame is)]
             (if frame
               (pave/queue-frame fq frame)
               (log/info "No frame?"))
             (recur (pave/read-frame is)))))
      (.setDaemon true)
      (.start))
    (doto
        (Thread.
         (fn []
           (let [g (.getGraphics view)
                 bi (BufferedImage.
                     (.getWidth view)
                     (.getHeight view)
                     BufferedImage/TYPE_INT_ARGB)
                 gbi (.createGraphics bi)]
             (loop [frame (pave/pull-frame fq 1000)]
               (when frame
                 (let [^BufferedImage image (decoder frame)]
                   (seesaw/invoke-now
                    (draw-image view gbi image)
                    (draw-hud view gbi drone)
                    (.drawImage g bi 0 0 nil))))
               (recur (pave/pull-frame fq 1000))))))
      (.setDaemon true)
      (.start))))


(defn -main [& args]
  (let [ui (make-ui)
        drone (ar-drone/make-drone)]
    (-> ui seesaw/pack! seesaw/show!)
    (seesaw/listen ui :key (make-key-controller drone))
    (connect-video-controller ui drone)
    (ar-drone/connect! drone)
    ;;(doseq [i (range 10)]
    ;;  (ar-drone/command drone :flat-trim)
    ; ; (Thread/sleep 500))
    ))
