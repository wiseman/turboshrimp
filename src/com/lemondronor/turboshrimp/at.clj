(ns com.lemondronor.turboshrimp.at
  (:require [clojure.string :as string])
  (:import (java.nio ByteBuffer)
           (clojure.lang PersistentVector)))

(set! *warn-on-reflection* true)


(defrecord AtCommand [type args blocks? options callback])


(defn serialize [seq-num at-command]
  (assert (instance? AtCommand at-command))
  (str "AT*"
       (:type at-command)
       "="
       (string/join "," (cons seq-num (:args at-command)))
       "\r"))


(defn at-encoded-float [f]
  (let [bbuffer (ByteBuffer/allocate 4)]
    (.put (.asFloatBuffer bbuffer) 0 f)
    (.get (.asIntBuffer bbuffer) 0)))


(def command-table (atom {}))

(defmacro defcommand [name args & body]
  `(swap! command-table
          assoc
          ~name
          {:name ~name
           :builder
           (fn ~args
             ~@body)}))


(defn commands-bytes [counter commands]
  (let [b (->> (map (fn [seqnum command]
                      (serialize seqnum command))
                    (iterate inc counter)
                    commands)
               (string/join)
               (.getBytes))]
    b))


(defn build-command [command & args]
  (if-let [cmd (@command-table command)]
    (try
      (apply (:builder cmd) args)
      (catch clojure.lang.ArityException e
        (throw (ex-info
                (str "Wrong number of arguments ("
                     (count args)
                     ") passed to drone command "
                     (pr-str command))
                {}
                e))))
    (throw (ex-info (str "Unknown command: " (pr-str command))
                    {:command command
                     :args args}))))


(defn raw [type args & [blocks? options callback]]
  (AtCommand. type args blocks? options callback))


(defcommand :ctrl [control-mode other-mode]
  (raw "CTRL" [control-mode other-mode] false nil nil))


(defn flags-value [flag-defs flags]
  (reduce (fn [v [flag set?]]
            (when (not (contains? flag-defs flag))
              (throw
               (ex-info (str "Unknown flag value: " flag) {})))
            (bit-or v (if set? (flag-defs flag) 0)))
          0
          flags))


(def ref-flags
  {:emergency (bit-shift-left 1 8)
   :fly (bit-shift-left 1 9)})


(defcommand :ref [options]
  (raw "REF" [(flags-value ref-flags options)] false nil nil))


(def pcmd-flags
  {:progressive (bit-shift-left 1 0)})


(def pcmd-aliases
  {:left              {:index 1 :invert true}
   :right             {:index 1 :invert false}
   :front             {:index 2 :invert true}
   :back              {:index 2 :invert false}
   :up                {:index 3 :invert false}
   :down              {:index 3 :invert true}
   :clockwise         {:index 4 :invert false}
   :counter-clockwise {:index 4 :invert true}})


(defcommand :pcmd [options]
  (let [prog-value (if (empty? options)
                     0
                     (:progressive pcmd-flags))
        args (reduce (fn [args [option value]]
                       (when (not (pcmd-aliases option))
                         (throw (ex-info
                                 (str "Unknown pcmd option: " option)
                                 {})))
                       (let [value (if (:invert (pcmd-aliases option))
                                     (- value)
                                     value)]
                         (assoc args
                           (:index (pcmd-aliases option))
                           (at-encoded-float value))))
                     [prog-value 0 0 0 0]
                     options)]
  (raw "PCMD" args false nil nil)))


(defcommand :calibrate [device-num]
  (raw "CALIB" [device-num] false nil nil))


(defcommand :flat-trim []
  (raw "FTRIM" [] false nil nil))


(defn stringize [v]
  (str "\"" v "\""))


(defcommand :config [key value & [callback]]
  (raw "CONFIG" [(stringize key) (stringize value)] true nil callback))


(def ^PersistentVector led-animations
  [:blink-green-red
   :blink-green,
   :blink-red,
   :blink-orange,
   :snake-green-red,
   :fire,
   :standard,
   :red,
   :green,
   :red-snake,
   :blank,
   :right-missile,
   :left-missile,
   :double-missile,
   :front-left-green-others-red,
   :front-right-green-others-red,
   :rear-right-green-others-red,
   :rear-left-green-others-red,
   :left-green-right-red,
   :left-red-right-green,
   :blink-standard])


(defcommand :animate-leds [& [name hz duration]]
  (let [name (or name :red-snake)
        id (.indexOf led-animations name)
        hz (or hz 2)
        duration (or duration 3)]
    (when (< id 0)
      (throw (ex-info (str "Unknown LED animation: " name) {})))
    (build-command
     :config
     "leds:leds_anim"
     (string/join "," [id (at-encoded-float hz) duration]))))


(def ^PersistentVector animations
  [:phi-m-30-deg,
   :phi-30-deg,
   :theta-m-30-deg,
   :theta3-0-deg,
   :theta-20-deg-yaw-200deg,
   :theta-20-deg-yaw-m-200deg,
   :turnaround,
   :turnaround-godown,
   :yaw-shake,
   :yaw-dance,
   :phi-dance,
   :theta-dance,
   :vz-dance,
   :wave,
   :phi-theta-mixed,
   :double-phi-theta-mixed,
   :flip-ahead,
   :flip-behind,
   :flip-left,
   :flip-right])


(defcommand :animate [name duration]
  (let [id (.indexOf animations name)]
    (when (< id 0)
      (throw (ex-info (str "Unknown animation: " name) {})))
    (build-command
     :config
     "control:flight_anim"
     (string/join "," [id duration]))))


(defcommand :navdata-demo []
  (build-command :config "general:navdata_demo" "FALSE"))
