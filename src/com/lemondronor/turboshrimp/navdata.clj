(ns com.lemondronor.turboshrimp.navdata
  (:require [gloss.core :as gloss]
            gloss.io
            [lazymap.core :as lazymap]
            [clojure.tools.logging :as log])
  (:import (java.net DatagramPacket DatagramSocket InetAddress)
           (java.nio ByteBuffer ByteOrder)
           (java.util Arrays)))

(set! *warn-on-reflection* true)


;; Codecs for vectors and matrices used by other codecs.

(gloss/defcodec vector3-codec
  (gloss/ordered-map
   :x :float32-le
   :y :float32-le
   :z :float32-le))

(gloss/defcodec matrix33-codec
  (gloss/ordered-map
   :m11 :float32-le
   :m12 :float32-le
   :m13 :float32-le
   :m21 :float32-le
   :m22 :float32-le
   :m23 :float32-le
   :m31 :float32-le
   :m32 :float32-le
   :m33 :float32-le))


(def control-states
  {0 :default, 1 :init, 2 :landed, 3 :flying, 4 :hovering, 5 :test,
   6 :trans-takeoff, 7 :trans-gotofix, 8 :trans-landing, 9 :trans-looping})


(def adc-data-frame-codec
  (gloss/compile-frame
   (gloss/ordered-map
    :version :uint32-le
    :data-frame (repeat 32 :ubyte))))

(defn parse-adc-data-frame-option [bb]
  (gloss.io/decode adc-data-frame-codec bb))


(def altitude-codec
  (gloss/compile-frame
   (gloss/ordered-map
    :vision :int32-le
    :velocity :float32-le
    :ref :int32-le
    :raw :int32-le
    :observer (gloss/ordered-map
               :acceleration :float32-le
               :altitude :float32-le
               :x vector3-codec
               :state :uint32-le)
    :estimated (gloss/ordered-map
                :vb (gloss/ordered-map
                     :x :float32-le
                     :y :float32-le)
                :state :uint32-le))))

(defn parse-altitude-option [bb]
  (gloss.io/decode altitude-codec bb))


;; Types of vision detections.
(def detection-types
  {0 :horizontal-deprecated,
   1 :vertical-deprecated,
   2 :horizontal-drone-shell
   3 :none-disabled
   4 :roundel-under-drone
   5 :oriented-roundel-under-drone
   6 :oriented-roundel-front-drone
   7 :stripe-ground
   8 :roundel-front-drone
   9 :stripe
   10 :multiple
   11 :cap-orange-green-front-drone
   12 :black-white-roundel
   13 :2nd-verion-shell-tag-front-drone
   14 :tower-side-front-camera})

(defn parse-control-state [v]
  (control-states (bit-shift-right v 16)))

;; Codec for the demo option.
(def demo-codec
  (gloss/compile-frame
   (gloss/ordered-map
    :control-state :uint32-le
    :battery-percentage :uint32-le
    :theta :float32-le
    :phi :float32-le
    :psi :float32-le
    :altitude :uint32-le
    :velocity vector3-codec
    :frame-index :uint32-le
    :detection (gloss/ordered-map
                :camera (gloss/ordered-map
                         :rotation matrix33-codec
                         :translation vector3-codec)
                :tag-index :uint32-le)
    :detect-camera-type :uint32-le
    :drone (gloss/ordered-map
            :camera (gloss/ordered-map
                     :rotation matrix33-codec
                     :translation vector3-codec)))
   identity
   (fn [demo]
     (assoc demo
       :control-state (parse-control-state (:control-state demo))
       :theta (float (/ (:theta demo) 1000))
       :phi (float (/ (:phi demo) 1000))
       :psi (float (/ (:psi demo) 1000))
       :altitude (float (/ (:altitude demo) 1000))
       :detect-camera-type (detection-types (:detect-camera-type demo))))))

(defn parse-demo-option [bb]
  (gloss.io/decode demo-codec bb))


(def euler-angles-codec
  (gloss/compile-frame
   (gloss/ordered-map
    :theta :float32-le
    :phi :float32-le)))

(defn parse-euler-angles-option [bb]
  (gloss.io/decode euler-angles-codec bb))


(defn >>> [x n]
  (bit-shift-right (bit-and 0xFFFFFFFF x) n))

(defn drone-time-to-seconds [time]
  (let [;; First 11 bits are seconds.
        seconds (>>> time 21)
        ;; Last 21 bits are microseconds.
        microseconds (>>> (bit-shift-left time 11) 11)]
    (+ seconds (/ microseconds 1000000.0))))


(def vision-codec
  (gloss/compile-frame
   (gloss/ordered-map
    :state :uint32-le
    :misc :int32-le
    :phi (gloss/ordered-map
          :trim :float32-le
          :ref-prop :float32-le)
    :theta (gloss/ordered-map
            :trim :float32-le
            :ref-prop :float32-le)
    :new-raw-picture :int32-le
    :capture (gloss/ordered-map
              :theta :float32-le
              :phi :float32-le
              :psi :float32-le
              :altitude :int32-le
              :time :uint32-le)
    :body-v vector3-codec
    :delta (gloss/ordered-map
            :phi :float32-le
            :theta :float32-le
            :psi :float32-le)
    :gold (gloss/ordered-map
           :defined :uint32-le
           :reset :uint32-le
           :x :float32-le
           :y :float32-le))
   identity
   (fn [v]
     (update-in v [:capture :time] drone-time-to-seconds))))

(defn parse-vision-option [bb]
  (gloss.io/decode vision-codec bb))


(def camera-sources
  {0 :horizontal
   1 :vertical
   2 :vertical-hsync})

(def max-vision-detections 4)

(def vision-detect-codec
  (gloss/compile-frame
   (gloss/ordered-map
    :num-detected :uint32-le
    :detections
    (gloss/ordered-map
     :type (repeat 4 :uint32-le)
     :xc (repeat 4 :uint32-le)
     :yc (repeat 4 :uint32-le)
     :width (repeat 4 :uint32-le)
     :height (repeat 4 :uint32-le)
     :dist (repeat 4 :uint32-le)
     :orientation-angle (repeat 4 :float32-le)
     :rotation (repeat 4 matrix33-codec)
     :translation (repeat 4 vector3-codec)
     :camera-source (repeat 4 :uint32-le)))
   identity
   (fn [vision]
     (let [v (-> vision
                 (update-in [:detections :type]
                            #(mapv detection-types %))
                 (update-in [:detections :camera-source]
                            #(mapv camera-sources %)))]
       (map (fn [i]
              (into {}
                    (for [k (keys (:detections v))]
                      [k (get-in (:detections v) [k i])])))
            (range (min
                    max-vision-detections
                    (:num-detected v))))))))

(defn parse-vision-detect-option [bb]
  (gloss.io/decode vision-detect-codec bb))


(def vision-perf-codec
  (gloss/compile-frame
   (gloss/ordered-map
    :szo :float32-le
    :corners :float32-le
    :compute :float32-le
    :tracking :float32-le
    :trans :float32-le
    :update :float32-le
    :custom (repeat 20 :float32-le))))

(defn parse-vision-perf-option [bb]
  (gloss.io/decode vision-perf-codec bb))


(def vision-of-codec
  (gloss/compile-frame
   (gloss/ordered-map
    :dx (repeat 5 :float32-le)
    :dy (repeat 5 :float32-le))))

(defn parse-vision-of-option [bb]
  (gloss.io/decode vision-of-codec bb))


(def vision-raw-codec
  (gloss/compile-frame
   (gloss/ordered-map
    :tx :float32-le
    :ty :float32-le
    :tz :float32-le)))

(defn parse-vision-raw-option [bb]
  (gloss.io/decode vision-raw-codec bb))


(def watchdog-codec
  (gloss/compile-frame
   :uint32-le))

(defn parse-watchdog-option [bb]
  (gloss.io/decode watchdog-codec bb))


(def time-codec
  (gloss/compile-frame
   :uint32-le
   identity
   drone-time-to-seconds))

(defn parse-time-option [bb]
  (gloss.io/decode time-codec bb))


(def trackers-send-codec
  (gloss/compile-frame
   (gloss/ordered-map
    :locked (repeat 30 :int32-le)
    :point (repeat 30 (gloss/ordered-map
                       :x :int32-le
                       :y :int32-le)))))

(defn parse-trackers-send-option [bb]
  (gloss.io/decode trackers-send-codec bb))


(def raw-measures-codec
  (gloss/compile-frame
   (gloss/ordered-map
    :accelerometers
    (gloss/ordered-map
     :x :uint16-le
     :y :uint16-le
     :z :uint16-le)
    :gyroscopes
     (gloss/ordered-map
      :x :int16-le
      :y :int16-le
      :z :int16-le)
     :gyroscopes-110
     (gloss/ordered-map
      :x :int16-le
      :y :int16-le)
     :battery-millivolts :uint32-le
     :us-echo
     (gloss/ordered-map
      :start :uint16-le
      :end :uint16-le
      :association :uint16-le
      :distance :uint16-le)
     :us-curve
     (gloss/ordered-map
      :time :uint16-le
      :value :uint16-le
      :ref :uint16-le)
     :echo
     (gloss/ordered-map
      :flag-ini :uint16-le
      :num :uint16-le
      :sum :uint32-le)
     :alt-temp-raw :int32-le
     :gradient :int16-le)))

(defn parse-raw-measures-option [bb]
  (gloss.io/decode raw-measures-codec bb))


(def rc-references-codec
  (gloss/compile-frame
   (gloss/ordered-map
    :pitch :int32-le
    :roll :int32-le
    :yaw :int32-le
    :gaz :int32-le
    :az :int32-le)))

(defn parse-rc-references-option [bb]
  (gloss.io/decode rc-references-codec bb))


(def references-codec
  (gloss/compile-frame
   (gloss/ordered-map
    :theta :int32-le
    :phi :int32-le
    :theta-i :int32-le
    :phi-i :int32-le
    :pitch :int32-le
    :roll :int32-le
    :yaw :int32-le
    :psi :int32-le
    :vx :float32-le
    :vy :float32-le
    :theta-mod :float32-le
    :phi-mod :float32-le
    :k-v-x :float32-le
    :k-v-y :float32-le
    :k-mode :float32-le
    :ui (gloss/ordered-map
         :time :float32-le
         :theta :float32-le
         :phi :float32-le
         :psi :float32-le
         :psi-accuracy :float32-le
         :seq :int32-le))))

(defn parse-references-option [bb]
  (gloss.io/decode references-codec bb))


(def trims-codec
  (gloss/compile-frame
   (gloss/ordered-map
    :angular-rates (gloss/ordered-map
                    :r :float32-le)
    :euler-angles (gloss/ordered-map
                   :theta :float32-le
                   :phi :float32-le))))

(defn parse-trims-option [bb]
  (gloss.io/decode trims-codec bb))


(def phys-measures-codec
  (gloss/compile-frame
   (gloss/ordered-map
    :temperature (gloss/ordered-map
                  :accelerometer :float32-le
                  :gyroscope :uint16-le)
    :accelerometers vector3-codec
    :gyroscopes vector3-codec
    :alim3v3 :uint32-le
    :vref-epson :uint32-le
    :vref-idg :uint32-le)))

(defn parse-phys-measures-option [bb]
  (gloss.io/decode phys-measures-codec bb))


(def pwm-codec
  (gloss/compile-frame
   (gloss/ordered-map
    :motors (repeat 4 :ubyte)
    :sat-motors (repeat 4 :ubyte)
    :gaz-feed-forward :float32-le
    :gaz-altitude :float32-le
    :altitude-integral :float32-le
    :vz-ref :float32-le
    :u-pitch :int32-le
    :u-roll :int32-le
    :u-yaw :int32-le
    :yaw-u-i :int32-le
    :u-pitch-planif :int32-le
    :u-roll-planif :int32-le
    :u-yaw-planif :int32-le
    :u-gaz-planif :int32-le
    :motor-currents (repeat 4 :uint16-le)
    :altitude-prop :float32-le
    :altitude-der :float32-le)))

(defn parse-pwm-option [bb]
  (gloss.io/decode pwm-codec bb))


(def wifi-codec
  (gloss/compile-frame
   :float32-le
   identity
   (fn [v]
     {:link-quality (- 1.0 v)})))


(defn parse-wifi-option [bb]
  (gloss.io/decode wifi-codec bb))

;; GPS structure from
;; https://github.com/paparazzi/paparazzi/blob/55e3d9d79119f81ed0b11a59487280becf13cf40/sw/airborne/boards/ardrone/at_com.h#L157

(def gps-sat-channel-codec
  (gloss/compile-frame
   (gloss/ordered-map
    :sat :ubyte
    :cn0 :ubyte)))

;; These definitions come from from
;; https://github.com/lesire/ardrone_autonomy/commit/a986b3380da8d9306407e2ebfe7e0f2cd5f97683
(def gps-codec
  (gloss/compile-frame
   (gloss/ordered-map
    :latitude :float64-le
    :longitude :float64-le
    :elevation :float64-le
    :hdop :float64-le
    :data-available :int32-le
    :zero-validated :int32-le
    :wpt-validated :int32-le
    :lat0 :float64-le
    :lon0 :float64-le
    :lat-fuse :float64-le
    :lon-fuse :float64-le
    :gps-state :uint32-le
    :x-traj :float32-le
    :x-ref :float32-le
    :y-traj :float32-le
    :y-ref :float32-le
    :theta-p :float32-le
    :phi-p :float32-le
    :theta-i :float32-le
    :phi-i :float32-le
    :theta-d :float32-le
    :phi-d :float32-le
    :vdop :float64-le
    :pdop :float64-le
    :speed :float32-le
    :last-frame-timestamp :uint32-le
    :degree :float32-le
    :degree-mag :float32-le
    :ehpe :float32-le
    :ehve :float32-le
    :c-n0 :float32-le
    :num-satellites :uint32-le
    :channels (repeat 12 gps-sat-channel-codec)
    :gps-plugged :int32-le
    :ephemeris-status :uint32-le
    :vx-traj :float32-le
    :vy-traj :float32-le
    :firmware-status :uint32-le)
   identity
   (fn [gps]
     (assoc gps :last-frame-timestamp
            (drone-time-to-seconds (:last-frame-timestamp gps))))))

(defn parse-gps-option [bb]
  (gloss.io/decode gps-codec bb))


(def gyros-offsets-codec
  vector3-codec)

(defn parse-gyros-offsets-option [bb]
  (gloss.io/decode gyros-offsets-codec bb))

(def magneto-codec
  (gloss/compile-frame
   (gloss/ordered-map
    :mx :int16-le
    :my :int16-le
    :mz :int16-le
    :raw vector3-codec
    :rectified vector3-codec
    :offset vector3-codec
    :heading (gloss/ordered-map
              :unwrapped :float32-le
              :gyro-unwrapped :float32-le
              :fusion-unwrapped :float32-le)
    :calibration-ok :ubyte
    :state :uint32-le
    :radius :float32-le
    :error (gloss/ordered-map
            :mean :float32-le
            :variance :float32-le))))

(defn parse-magneto-option [bb]
  (gloss.io/decode magneto-codec bb))

(def state-masks
  [ {:name :flying             :mask 0  :values [:landed :flying]}
    {:name :video              :mask 1  :values [:off :on]}
    {:name :vision             :mask 2  :values [:off :on]}
    {:name :control            :mask 3  :values [:euler-angles :angular-speed]}
    {:name :altitude-control   :mask 4  :values [:off :on]}
    {:name :user-feedback      :mask 5  :values [:off :on]}
    {:name :command-ack        :mask 6  :values [:none :received]}
    {:name :camera             :mask 7  :values [:not-ready :ready]}
    {:name :travelling         :mask 8  :values [:off :on]}
    {:name :usb                :mask 9  :values [:not-ready :ready]}
    {:name :demo               :mask 10 :values [:off :on]}
    {:name :bootstrap          :mask 11 :values [:off :on]}
    {:name :motors             :mask 12 :values [:ok :motor-problem]}
    {:name :communication      :mask 13 :values [:ok :communication-lost]}
    {:name :software           :mask 14 :values [:ok :software-fault]}
    {:name :battery            :mask 15 :values [:ok :too-low]}
    {:name :emergency-landing  :mask 16 :values [:off :on]}
    {:name :timer              :mask 17 :values [:not-elapsed :elapsed]}
    {:name :magneto            :mask 18 :values [:ok :needs-calibration]}
    {:name :angles             :mask 19 :values [:ok :out-of-range]}
    {:name :wind               :mask 20 :values [:ok :too-much]}
    {:name :ultrasound         :mask 21 :values [:ok :deaf]}
    {:name :cutout             :mask 22 :values [:ok :detected]}
    {:name :pic-version        :mask 23 :values [:bad-version :ok]}
    {:name :atcodec-thread     :mask 24 :values [:off :on]}
    {:name :navdata-thread     :mask 25 :values [:off :on]}
    {:name :video-thread       :mask 26 :values [:off :on]}
    {:name :acquisition-thread :mask 27 :values [:off :on]}
    {:name :ctrl-watchdog      :mask 28 :values [:ok :delay]}
    {:name :adc-watchdog       :mask 29 :values [:ok :delay]}
    {:name :com-watchdog       :mask 30 :values [:ok :problem]}
    {:name :emergency          :mask 31 :values [:ok :detected]}
    ])

(defn parse-nav-state [state]
  (reduce
   #(let  [{:keys [name mask values]} %2
           bvalue (bit-and state (bit-shift-left 1 mask))]
      (conj %1 {name
                (if (= 0 bvalue) (first values) (last values))}))
   {}
   state-masks))


;; Map from packet option ID to symbolic type.
(def which-option-type
  {0 :demo
   1 :time
   2 :raw-measures
   3 :phys-measures
   4 :gyros-offsets
   5 :euler-angles
   6 :references
   7 :trims
   8 :rc-references
   9 :pwm
   10 :altitude
   11 :vision-raw
   12 :vision-of
   13 :vision
   14 :vision-perf
   15 :trackers-send
   16 :vision-detect
   17 :watchdog
   18 :adc-data-frame
   22 :magneto
   26 :wifi
   27 :gps
   0xffff :checksum})

(def option-parsers
  {:adc-data-frame parse-adc-data-frame-option
   :altitude parse-altitude-option
   :demo parse-demo-option
   :euler-angles parse-euler-angles-option
   :gps parse-gps-option
   :gyros-offsets parse-gyros-offsets-option
   :magneto parse-magneto-option
   :phys-measures parse-phys-measures-option
   :pwm parse-pwm-option
   :raw-measures parse-raw-measures-option
   :rc-references parse-rc-references-option
   :references parse-references-option
   :time parse-time-option
   :trackers-send parse-trackers-send-option
   :trims parse-trims-option
   :vision parse-vision-option
   :vision-detect parse-vision-detect-option
   :vision-of parse-vision-of-option
   :vision-perf parse-vision-perf-option
   :vision-raw parse-vision-raw-option
   :watchdog  parse-watchdog-option
   :wifi parse-wifi-option})

(defn parse-option [bb option-type]
  (log/debug "Parsing navdata option" option-type)
  (let [parser (option-parsers option-type)]
    (parser bb)))

(defn slice-byte-buffer [^ByteBuffer bb ^long offset ^long len]
  (let [ba ^"[B" (Arrays/copyOfRange
                  ^"[B" (.array bb)
                  offset
                  (+ offset len))
        bb (ByteBuffer/wrap ba)]
    (.order bb ByteOrder/LITTLE_ENDIAN)
    bb))

(defn get-ushort [^ByteBuffer bb]
  (bit-and 0xFFFF (Integer. (int (.getShort bb)))))

(defn get-uint [^ByteBuffer bb]
  (bit-and 0xFFFFFFFF (Long. (long (.getInt bb)))))

(defn check-checksum [value bb]
  (log/debug "CHECKSUM"))

(defn parse-options [^ByteBuffer bb]
  (loop [options (lazymap/lazy-hash-map)]
    (let [option-header (get-ushort bb)
          option-type (which-option-type option-header)
          option-size (get-ushort bb)]
      ;; From the developer guide: "The checksum is always the last
      ;; option (data block) in the navdata packet."
      (if (= option-type :checksum)
        (do
          (check-checksum (get-uint bb) bb)
          options)
        (let [old-pos (.position bb)
              new-pos (+ old-pos (- option-size 4))]
          (.position bb new-pos)
          (if option-type
            (recur
             (lazymap/lazy-assoc
              options
              option-type
              (parse-option
               (slice-byte-buffer bb old-pos (- option-size 4))
               option-type)))
            (recur options)))))))

(def navdata-codec
  (gloss/compile-frame
   (gloss/ordered-map
    :header :uint32-le
    :state :uint32-le
    :seqnum :uint32-le
    :vision-flag :uint32-le)))


(defn parse-navdata [navdata-bytes]
  (log/debug "-- parse-navdata")
  (let [^ByteBuffer bb (doto ^ByteBuffer (gloss.io/to-byte-buffer navdata-bytes)
                         (.order ByteOrder/LITTLE_ENDIAN))
        header (get-uint bb)
        state (get-uint bb)
        seqnum (get-uint bb)
        vision-flag (= (get-uint bb) 1)
        pstate (parse-nav-state state)
        options (parse-options bb)]
    (assoc options
      :header header
      :seq-num seqnum
      :vision-flag vision-flag
      :state pstate)))



(defn options-bytes-seq [^ByteBuffer bb]
  (if (zero? (.remaining bb))
    '()
    (let [option-header (get-ushort bb)
          option-size (get-ushort bb)]
      (if (pos? option-size)
        (cons [(or (which-option-type option-header) option-header)
               (slice-byte-buffer bb 0 option-size)]
              (lazy-seq
               (options-bytes-seq
                (slice-byte-buffer
                 bb option-size (+ 4 (- (.remaining bb) option-size))))))
        (do
          (log/info "Aborting 0-length option.")
          '())))))


(defn navdata-bytes-seq [navdata-bytes]
  (let [^ByteBuffer bb (doto ^ByteBuffer (gloss.io/to-byte-buffer navdata-bytes)
                             (.order ByteOrder/LITTLE_ENDIAN))]
    [(slice-byte-buffer bb 0 16)
     (options-bytes-seq
      (slice-byte-buffer bb 16 (- (count navdata-bytes) 16)))]))



(defn new-datagram-packet [^bytes data ^InetAddress host ^long port]
  (DatagramPacket. data (count data) host port))

(defn send-navdata  [^DatagramSocket navdata-socket datagram-packet]
  (.send navdata-socket datagram-packet))

(defn receive-navdata  [^DatagramSocket navdata-socket datagram-packet]
  (.receive navdata-socket datagram-packet))

(defn get-navdata-bytes  [^DatagramPacket datagram-packet]
  (.getData datagram-packet))
