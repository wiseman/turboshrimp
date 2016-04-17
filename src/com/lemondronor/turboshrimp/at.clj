(ns com.lemondronor.turboshrimp.at
  "Code related to low-level drone commands."
  (:require [clojure.string :as string])
  (:import (clojure.lang PersistentVector)
           (java.nio ByteBuffer)))

(set! *warn-on-reflection* true)


(defrecord ^:no-doc AtCommand [type args blocks? options callback])

(alter-meta! (var ->AtCommand) #(assoc % :private true))
(alter-meta! (var map->AtCommand) #(assoc % :private true))


(defn ^:no-doc serialize [seq-num at-command]
  (assert (instance? AtCommand at-command))
  (str "AT*"
       (:type at-command)
       "="
       (string/join "," (cons seq-num (:args at-command)))
       "\r"))


(defn ^:no-doc at-encoded-float [f]
  (let [bbuffer (ByteBuffer/allocate 4)]
    (.put (.asFloatBuffer bbuffer) 0 f)
    (.get (.asIntBuffer bbuffer) 0)))


(def ^:no-doc command-table (atom {}))

(defmacro ^:no-doc defcommand [name doc args & body]
  `(swap! command-table
          assoc
          ~name
          {:name ~name
           :doc ~doc
           :args '~args
           :builder
           (fn ~args
             ~@body)}))


(defn ^:no-doc commands-bytes [counter commands]
  (let [b (->> (map (fn [seqnum command]
                      (serialize seqnum command))
                    (iterate inc counter)
                    commands)
               (string/join)
               (.getBytes))]
    b))


(defn ^:no-doc build-command [command & args]
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


(defn ^:no-doc raw [type args & [blocks? options callback]]
  (AtCommand. type args blocks? options callback))


(defcommand :ctrl
  "This is a command that is not well documented in the AR.Drone
  SDK, and that you probably won't ever need to use.

  It can be used to clear the CONFIG ACK flag for \"muticonfig\"
  settings—see https://gist.github.com/bkw/4416785 for more
  information."
  [control-mode other-mode]
  (raw "CTRL" [control-mode other-mode] false nil nil))


(defn ^:no-doc flags-value [flag-defs flags]
  (reduce (fn [v [flag set?]]
            (when (not (contains? flag-defs flag))
              (throw
               (ex-info (str "Unknown flag value: " flag) {})))
            (bit-or v (if set? (flag-defs flag) 0)))
          0
          flags))


(def ^:no-doc ref-flags
  {:emergency (bit-shift-left 1 8)
   :fly (bit-shift-left 1 9)})


(defcommand :ref
  "Controls the basic behaviour of the drone (take-off/landing,
  emergency stop/reset). You shouldn't ever need to use this, because
  there are higher level functions already defined for those
  behaviors. See [[takeoff]], [[land]], and [[disable-emergency]]."
  [options]
  (raw "REF" [(flags-value ref-flags options)] false nil nil))


(def ^:no-doc pcmd-flags
  {:progressive (bit-shift-left 1 0)})


(def ^:no-doc pcmd-aliases
  {:left              {:index 1 :invert true}
   :right             {:index 1 :invert false}
   :front             {:index 2 :invert true}
   :back              {:index 2 :invert false}
   :up                {:index 3 :invert false}
   :down              {:index 3 :invert true}
   :clockwise         {:index 4 :invert false}
   :counter-clockwise {:index 4 :invert true}})


(defcommand :pcmd
  "Makes the drone move (translate and rotate). You shouldn'e ever
  need to use this, because there are higher level functions already
  defined for those behaviors.
  See [[up]], [[down]], [[left]], [[right]], [[front]], [[back]], [[clockwise]], [[counter-clockwise]]."
  [options]
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


(defcommand :calibrate
  "Tells the drone to calibrate its magnetometer.

  This command MUST be sent when the AR.Drone is flying.

  On receipt of this command, the drone will automatically calibrate
  its magnetometer by rotating itself and then stopping.

  `device-id` is the ID of the device to calibrate. The magnetometer
  is device ID 0."
  [device-id]
  (raw "CALIB" [device-id] false nil nil))


(defcommand :flat-trim
  "Sets a reference of the horizontal plane for the drone internal
  control system.

  It must be called after each drone start up, while making sure the
  drone actually sits on a horizontal ground. Not doing so before
  taking-off will result in the drone not being able to stabilize
  itself when flying, as it would not be able to know its actual tilt.
  This command MUST NOT be sent when the AR.Drone is flying."
  []
  (raw "FTRIM" [] false nil nil))


(defn ^:no-doc stringize [v]
  (str "\"" v "\""))


(defcommand :config
  "Configures drone options.

  You shouldn't need to use this command because there are higher
  level functions already defined for most behaviors. See
  [[navdata-demo]], [[navdata-options]], [[animate]], [[animate-leds]],
  [[switch-camera]], and [[set-camera-framerate]]."
  [key value & [callback]]
  (raw "CONFIG" [(stringize key) (stringize value)] true nil callback))


(def navdata-flags
  "A map whose keys are valid navdata flags that can be passed
  to [[navdata-options]]."
  {:demo 0
   :time 1
   :raw-measures 2
   :phys-measures 3
   :gyros-offsets 4
   :euler-angles 5
   :references 6
   :trims 7
   :rc-references 8
   :pwm 9
   :altitude 10
   :vision-raw 11
   :vision-of 12
   :vision 13
   :vision-perf 14
   :trackers-send 15
   :vision-detect 16
   :watchdog 17
   :adc-data-frame 18
   :video-stream 19
   :games 20
   :pressure-raw 21
   :magneto 22
   :wind-speed 23
   :kalman-pressure 24
   :hdvideo-stream 25
   :wifi 26
   :zimmu-3000 27
   :gps 27})


(def default-navdata-options
  "A vector containing commonly used navdata options.

  Has the value `[:demo :vision-detect :magneto :gps]`.
  See [[navdata-options]] for more information."
  [:demo :vision-detect :magneto :gps])


(defcommand :navdata-options
  "When using [[navdata-demo]] to enable demo telemetry, this
  function selects which navdata packets to include.

  `flags` is a sequence of option keywords. [[navdata-flags]] contains
  all valid options. The most common set of options is predefined
  as [[at/default-navdata-options]].

  Examples:
  ```
  (navdata-options my-drone default-navdata-options)
  (navdata-options my-drone [:demo :gps])
  ```

  Here's a list of allowed flags and their meanings:

  | Flag               | Description                                    |
  |--------------------|------------------------------------------------|
  | `:demo`            | Demo navdata                                   |
  | `:time`            | Timestamp                                      |
  | `:raw-measures`    | Raw sensors measurements                       |
  | `:phys-measures`   | Gyros?                                         |
  | `:gyros-offsets`   | Gyro offsets?                                  |
  | `:euler-angles`    | Drone attitude in euler angles                 |
  | `:references`      | Attitude?                                      |
  | `:trims`           | Trim?                                          |
  | `:rc-references`   | Controller?                                    |
  | `:pwm`             | Motor control?                                 |
  | `:altitude`        | Altitude data                                  |
  | `:vision-raw`      | Vision?                                        |
  | `:vision-of`       | Vision offsets?                                |
  | `:vision`          | Vision?                                        |
  | `:vision-perf`     | Vision performance/timing?                     |
  | `:trackers-send`   | Vision tracking?                               |
  | `:vision-detect`   | Vision-detected tag information                |
  | `:watchdog`        | Watchdog timer?                                |
  | `:adc-data-frame`  | A/D converter?                                 |
  | `:video-stream`    | Video stream statistics                        |
  | `:games`           | Game?                                          |
  | `:pressure-raw`    | Raw barometric sensor data?                    |
  | `:magneto`         | Magnetometer data                              |
  | `:wind-speed`      | Estimate wind data                             |
  | `:kalman-pressure` | ?                                              |
  | `:hdvideo-stream`  | Statistics on HD video stored on flash drive   |
  | `:wifi`            | Statistics on the wireless network link        |
  | `:zimmu-3000`      | See `:gps`                                     |
  | `:gps`             | GPS data                                       |"
  [flags]
  (build-command
   :config
   "general:navdata_options"
   (reduce #(let [bit (or (navdata-flags %2) %2)]
              (bit-or %1 (bit-shift-left 1 bit)))
           0
           flags)))


(def ^PersistentVector led-animations
  "Vector of valid LED animations for use
  with [[turboshrimp/animate-leds]]."
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


(defcommand :animate-leds
  "Animates the drone's LED lights.

  `name`, if supplied, must be a symbol specifying a valid LED
  animation. `hz`, if supplied, is the frequency in Hz at which the
  animation will run. `duration`, if supplied, is how long the
  animation should run for, in seconds.

  These are the valid LED animations: `:blink-green-red`,
  `:blink-green`, `:blink-red`. `:blink-orange`, `:snake-green-red`,
  `:fire`, `:standard`, `:red`, `:green`, `:red-snake`, `:blank`,
  `:right-missile`, `:left-missile`, `:double-missile`,
  `:front-left-green-others-red`, `:front-right-green-others-red`,
  `:rear-right-green-others-red`, `:rear-left-green-others-red`,
  `:left-green-right-red`, `:left-red-right-green`, `:blink-standard`."
  [& [name hz duration]]
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
  "A vector containing valid animations for use
  with [[turboshrimp/animate]]."
  [:phi-m-30-deg,
   :phi-30-deg,
   :theta-m-30-deg,
   :theta-30-deg,
   :theta-20-deg-yaw-200-deg,
   :theta-20-deg-yaw-m-200-deg,
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


(def default-animation-duration
  "A map from flight animation symbol to default animation duration,
  in milliseconds. See [[turboshrimp/animate]]."
  {:phi-m-30-deg 1000,
   :phi-30-deg 1000,
   :theta-m-30-deg 1000,
   :theta-30-deg 1000,
   :theta-20-deg-yaw-200-deg 1000,
   :theta-20-deg-yaw-m-200-deg 1000,
   :turnaround 5000,
   :turnaround-godown 5000,
   :yaw-shake 2000,
   :yaw-dance 5000,
   :phi-dance 5000,
   :theta-dance 5000,
   :vz-dance 5000,
   :wave 5000,
   :phi-theta-mixed 5000,
   :double-phi-theta-mixed 5000,
   :flip-ahead 15,
   :flip-behind 15,
   :flip-left 15,
   :flip-right 15})


(defcommand :animate
  "Commands the drone to perform a flight animation.

  `name` is a symbol specifying a valid flight animation. `duration`
  is the duration of the animation in milliseconds.

  Example:
  ```
  (animate my-drone :yaw-shake (default-animation-duration :yaw-shake)
  ```

  These are the valid flight animations: `:phi-m-30-deg`,
  `:phi-30-deg`, `:theta-m-30-deg`, `:theta-30-deg`,
  `:theta-20-deg-yaw-200-deg`, `:theta-20-deg-yaw-m-200-deg`,
  `:turnaround`, `:turnaround-godown`, `:yaw-shake`, `:yaw-dance`,
  `:phi-dance`, `:theta-dance`, `:vz-dance`, `:wave`,
  `:phi-theta-mixed`, `:double-phi-theta-mixed`, `:flip-ahead`,
  `:flip-behind`, `:flip-left`, `:flip-right`.

  You can get the default duration for each flight animation from the
  map [[at/default-animation-duration]].

  The YouTube video [\"Drone Ace Flight Animation Controls for the
  AR.Drone\"](https://www.youtube.com/watch?v=toekPsOeqlQ) shows most
  of the animations."
  [name duration]
  (let [id (.indexOf animations name)]
    (when (< id 0)
      (throw (ex-info (str "Unknown animation: " name) {})))
    (build-command
     :config
     "control:flight_anim"
     (string/join "," [id duration]))))


(defcommand :navdata-demo
  "Enables or disables extended \"demo\" telemetry data."
  [enabled?]
  (build-command
   :config "general:navdata_demo"
   (if enabled? "TRUE" "FALSE")))


(defcommand :switch-camera
  "Switches the video stream between the forward-facing and
  downward-facing cameras.

  `direction` must be `:forward` or `:down`."
  [direction]
  (build-command
   :config "video:video_channel"
   (case direction
     :forward 0
     :down 3)))


(defcommand :set-camera-framerate
  "Sets the video stream frame rate."
  [fps]
  (build-command :config "video:codec_fps" fps))
