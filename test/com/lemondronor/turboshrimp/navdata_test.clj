(ns com.lemondronor.turboshrimp.navdata-test
  (:require [clojure.java.io :as io]
            [clojure.test :refer :all]
            [com.lemondronor.turboshrimp.navdata :as navdata]
            [com.lemonodor.xio :as xio])
  (:import (java.nio ByteBuffer ByteOrder)))

;; matrix 33 is 9 floats
;; vector 31 is 3 floats
(def b-matrix33  (vec (repeat (* 9 4) 0 )))
(def b-vector31  (vec (repeat (* 3 4) 0 )))

(def b-header [-120 119 102 85])
(def b-state [-48 4 -128 15])
(def b-seqnum [102 3 0 0])
(def b-vision [0 0 0 0])
(def b-demo-option-id [0 0])
(def b-demo-option-size [-108 0])
(def b-demo-control-state [0 0 2 0])
(def b-demo-battery [100 0 0 0])
(def b-demo-pitch [0 96 -122 -60])
(def b-demo-roll [0 -128 53 -59])
(def b-demo-yaw [0 0 87 -61])
(def b-demo-altitude [0 0 0 0])
(def b-demo-velocity-x [0 0 0 0])
(def b-demo-velocity-y [0 0 0 0])
(def b-demo-velocity-z [0 0 0 0])
(def b-demo-num-frames [0 0 0 0])
(def b-demo-detect-camera-rot b-matrix33)
(def b-demo-detect-camera-trans b-vector31)
(def b-demo-detect-tag-index [0 0 0 0])
(def b-demo-detect-camera-type [4 0 0 0])
(def b-demo-drone-camera-rot b-matrix33)
(def b-demo-drone-camera-trans b-vector31)
(def b-demo-option (flatten (conj b-demo-option-id b-demo-option-size
                                  b-demo-control-state b-demo-battery
                                  b-demo-pitch b-demo-roll b-demo-yaw
                                  b-demo-altitude b-demo-velocity-x
                                  b-demo-velocity-y b-demo-velocity-z
                                  b-demo-num-frames
                                  b-demo-detect-camera-rot b-demo-detect-camera-trans
                                  b-demo-detect-tag-index
                                  b-demo-detect-camera-type b-demo-drone-camera-rot
                                  b-demo-drone-camera-trans)))
(def b-vision-detect-option-id [16 0])
(def b-vision-detect-option-size [72 1])
(def b-vision-detect-num-tags-detected [2 0 0 0])
(def b-vision-detect-type [1 0 0 0 2 0 0 0 3 0 0 0 4 0 0 0])
(def b-vision-detect-xc [1 0 0 0 2 0 0 0 3 0 0 0 4 0 0 0])
(def b-vision-detect-yc [1 0 0 0 2 0 0 0 3 0 0 0 4 0 0 0])
(def b-vision-detect-width [1 0 0 0 2 0 0 0 3 0 0 0 4 0 0 0])
(def b-vision-detect-height [1 0 0 0 2 0 0 0 3 0 0 0 4 0 0 0])
(def b-vision-detect-dist [1 0 0 0 2 0 0 0 3 0 0 0 4 0 0 0])
(def b-vision-detect-orient-angle [0 96 -122 -60 0 96 -122 -60 0 96 -122 -60 0 96 -122 -60])
(def b-vision-detect-rotation (flatten (conj b-matrix33  b-matrix33  b-matrix33  b-matrix33)))
(def b-vision-detect-translation (flatten (conj b-vector31 b-vector31 b-vector31 b-vector31)))
(def b-vision-detect-camera-source [1 0 0 0 2 0 0 0 2 0 0 0 2 0 0 0])
(def b-vision-detect-option (flatten (conj b-vision-detect-option-id b-vision-detect-option-size
                                    b-vision-detect-num-tags-detected
                                    b-vision-detect-type b-vision-detect-xc b-vision-detect-yc
                                    b-vision-detect-width b-vision-detect-height b-vision-detect-dist
                                    b-vision-detect-orient-angle b-vision-detect-rotation b-vision-detect-translation
                                    b-vision-detect-camera-source)))
(def b-checksum-option-id [-1 -1])
(def b-checksum-option-size [0x08 0x00])
(def b-checksum-option-checksum [0x08 0x10 0x00 0x00])
(def b-checksum-option
  (flatten (conj b-checksum-option-id b-checksum-option-size
                 b-checksum-option-checksum)))

(def header (map byte [-120 119 102 85]))
(def nav-input
  (byte-array (map byte (flatten (conj b-header b-state b-seqnum b-vision
                                       b-demo-option b-vision-detect-option
                                       b-checksum-option)))))


(deftest navdata-unit-tests
  (testing "about parse-nav-state"
    (testing "parse-nav-state"
      (let [state 260048080
            result (navdata/parse-nav-state state)
            {:keys [flying video vision control altitude-control
                    user-feedback command-ack camera travelling
                    usb demo bootstrap motors communication
                    software battery emergency-landing timer
                    magneto angles wind ultrasound cutout
                    pic-version atcodec-thread navdata-thread
                    video-thread acquisition-thread ctrl-watchdog
                    adc-watchdog com-watchdog emergency]} result]
        (is (= flying :landed))
        (is (= video :off))
        (is (= vision :off))
        (is (= control :euler-angles))
        (is (= altitude-control :on))
        (is (= user-feedback :off))
        (is (= command-ack :received))
        (is (= camera :ready))
        (is (= travelling :off))
        (is (= usb :not-ready))
        (is (= demo :on))
        (is (= bootstrap :off))
        (is (= motors :ok))
        (is (= communication :ok))
        (is (= software :ok))
        (is (= battery :ok))
        (is (= emergency-landing :off))
        (is (= timer :not-elapsed))
        (is (= magneto :ok))
        (is (= angles :ok))
        (is (= wind :ok))
        (is (= ultrasound :ok))
        (is (= cutout :ok))
        (is (= pic-version :ok))
        (is (= atcodec-thread :on))
        (is (= navdata-thread :on))
        (is (= video-thread :on))
        (is (= acquisition-thread :on))
        (is (= ctrl-watchdog :ok))
        (is (= adc-watchdog :ok))
        (is (= com-watchdog :ok))
        (is (= emergency :ok)))))

  (testing "which-option-type"
    (is (= (navdata/which-option-type 0) :demo))
    (is (= (navdata/which-option-type 16) :vision-detect))
    (is (= (navdata/which-option-type 2342342) nil)))

  (testing "about parse-control-state"
    (testing "parse-control-state"
      (let [bb (doto (gloss.io/to-byte-buffer b-demo-control-state)
                            (.order ByteOrder/LITTLE_ENDIAN))
            control-state (.getInt bb)]
        (is (= (navdata/parse-control-state control-state) :landed)))))

  (testing "about parse-demo-option"
    (testing "parse-demo-option"
      (let [bb (doto (gloss.io/to-byte-buffer
                      ;; Skip past the option ID and option size.
                      (drop 4 b-demo-option))
                 (.order ByteOrder/LITTLE_ENDIAN))
            option (navdata/parse-demo-option bb)]
        (is (= (:control-state option) :landed))
        (is (= (:battery-percentage option) 100))
        (is (= (:theta option) (float -1.075)))
        (is (= (:phi option) (float -2.904)))
        (is (= (:psi option) (float -0.215)))
        (is (= (:altitude option) 0.0))
        (is (= (:velocity option) {:x 0.0 :y 0.0 :z 0.0}))
        (is (= (:detect-camera-type option) :roundel-under-drone)))))

  (testing "about parse-vision-detect-option"
    (let [detections
          (navdata/parse-vision-detect-option
           (doto (gloss.io/to-byte-buffer
                  ;; Skip past the option ID and option size.
                  (drop 4
                        b-vision-detect-option))
             (.order ByteOrder/LITTLE_ENDIAN)))]
      (is (= (count detections) 2))
      (testing "first detection"
        (let [det (nth detections 0)]
          (is (= (:type det) :vertical-deprecated))
          (is (= (:xc det) 1))
          (is (= (:yc det) 1))
          (is (= (:width det) 1))
          (is (= (:height det) 1))
          (is (= (:dist det) 1))
          (is (= (:orientation-angle det) -1075.0))
          (is (= (:camera-source det) :vertical))
          (is (= (:translation det) {:x 0.0 :y 0.0 :z 0.0}))
          (is (= (:rotation det)
                 {:m11 0.0, :m12 0.0, :m13 0.0,
                  :m21 0.0, :m22 0.0, :m23 0.0,
                  :m31 0.0, :m32 0.0, :m33 0.0}))))
      (testing "second detection"
        (let [det (nth detections 1)]
          (is (= (:type det) :horizontal-drone-shell))
          (is (= (:xc det) 2))
          (is (= (:yc det) 2))
          (is (= (:width det) 2))
          (is (= (:height det) 2))
          (is (= (:dist det) 2))
          (is (= (:orientation-angle det) -1075.0))
          (is (= (:camera-source det) :vertical-hsync))
          (is (= (:translation det) {:x 0.0 :y 0.0 :z 0.0}))
          (is (= (:rotation det) {:m11 0.0, :m12 0.0, :m13 0.0,
                                  :m21 0.0, :m22 0.0, :m23 0.0,
                                  :m31 0.0, :m32 0.0, :m33 0.0}))))))

  (testing "about parse-navdata"
    (testing "parse-navdata"
      (testing "hand-crafted input"
        (let [navdata (navdata/parse-navdata nav-input)]
          (is (= (:header navdata) 0x55667788))
          (is (= (:seq-num navdata) 870))
          (is (= (:vision-flag navdata) false))
          (testing "state"
            (let [state (:state navdata)]
              (is (= (:battery state) :ok))
              (is (= (:flying state) :landed))))
          (testing "demo"
            (let [demo (:demo navdata)]
              (is (= (:control-state demo) :landed))
              (is (= (:battery-percentage demo) 100))
              (is (= (:theta demo) (float -1.075)))
              (is (= (:phi demo) (float -2.904)))
              (is (= (:psi demo) (float -0.215)))
              (is (= (:altitude demo) 0.0))
              (is (= (:velocity demo) {:x 0.0 :y 0.0 :z 0.0})))))))))


(defn test-adc-data-frame-option [navdata]
  (testing "adc-data-frame option"
    (let [adc (:adc-data-frame navdata)]
      (is (= (:version adc) 0))
      (is (= (:data-frame adc) (repeat 32 0))))))


(defn test-gps-option [navdata]
  (testing "gps option"
    (let [gps (:gps navdata)]
      (are [x y] (= x y)
           (:latitude gps) 34.0905016
           (:longitude gps) -118.2766877
           (:elevation gps) 122.64
           (:hdop gps) 1.0
           (:data-available gps) 7
           (:zero-validated gps) 1
           (:wpt-validated gps) 0
           (:lat0 gps) 34.0905016
           (:lon0 gps) -118.2766877
           (:lat-fuse gps) 34.0904833
           (:lon-fuse gps) -118.2766982
           (:gps-state gps) 1
           (:x-traj gps) 0.0
           (:x-ref gps) 0.0
           (:y-traj gps) 0.0
           (:y-ref gps) 0.0
           (:theta-p gps) 0.0
           (:phi-p gps) 0.0
           (:theta-i gps) 0.0
           (:phi-i gps) 0.0
           (:theta-d gps) 0.0
           (:phi-d gps) 0.0
           (:vdop gps) 0.0
           (:pdop gps) 0.0
           (:speed gps) (float 0.1)
           (:last-frame-timestamp gps) 2.409591
           (:degree gps) (float 141.01)
           (:degree-mag gps) 0.0
           (:ehpe gps) (float 8.26)
           (:ehve gps) (float 0.42999998)
           (:c-n0 gps) 28.0
           (:num-satellites gps) 9
           (:channels gps) [{:cn0 26, :sat 10}
                            {:cn0 21, :sat 5}
                            {:cn0 27, :sat 8}
                            {:cn0 17, :sat 3}
                            {:cn0 18, :sat 13}
                            {:cn0 32, :sat 7}
                            {:cn0 23, :sat 9}
                            {:cn0 9, :sat 27}
                            {:cn0 19, :sat 19}
                            {:cn0 29, :sat 28}
                            {:cn0 26, :sat 30}
                            {:cn0 0, :sat 138}]
           (:gps-plugged gps) 1
           (:ephemeris-status gps) 73
           (:vx-traj gps) 0.0
           (:vy-traj gps) 0.0
           (:firmware-status gps) 1))))


(defn test-trackers-send-option [navdata]
  (testing "trackers-send option"
    (let [ts (:trackers-send navdata)]
      (is (= (:locked ts) (repeat 30 0)))
      (is (= (:point ts) (repeat 30 {:x 0 :y 0}))))))


(defn test-vision-option [navdata]
  (testing "vision option"
    (let [v (:vision navdata)]
      (are [x y] (= x y)
           (:state v) 2
           (:misc v) 0
           (:phi v) {:trim 0.0 :ref-prop 0.0}
           (:theta v) {:trim 0.0 :ref-prop 0.0}
           (:new-raw-picture v) 0
           (:capture v) {:theta (float 0.05190306529402733)
                         :phi (float 0.009620788507163525)
                         :psi (float 0.033727407455444336)
                         :altitude 243
                         :time 0.362969}
           (:body-v v) {:x (float 0.05845191329717636)
                        :y (float -0.8817280530929565)
                        :z (float 0.011505687609314919)}
           (:delta v) {:phi 0.0
                       :theta 0.0
                       :psi 0.0}
           (:gold v) {:defined 0
                      :reset 0
                      :x 0.0
                      :y 0.0}))))


(defn test-vision-perf-option [navdata]
  (testing "vision-perf option"
    (let [v (:vision-perf navdata)]
      (are [x y] (= x y)
           (:szo v) 0.0
           (:corners v) 0.0
           (:compute v) 0.0
           (:tracking v) 0.0
           (:trans v) 0.0
           (:update v) 0.0
           (:custom v) [0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0
                        0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0]))))


(defn test-watchdog-option [navdata]
  (testing "watchdog option"
    (let [w (:watchdog navdata)]
      (is (= w 4822)))))


(deftest navdata-specimen-tests
  (testing "parse-navdata on specimen"
    (let [navdata-bytes (xio/binary-slurp (io/resource "navdata.bin"))]
      ;;(println "Benchmarking parse-navdata")
      ;;(criterium/bench (parse-navdata navdata-bytes))
      ;;(criterium/bench (:gps (parse-navdata navdata-bytes)))
      (let [navdata (navdata/parse-navdata navdata-bytes)]

        (testing "navdata"
          (are [x y] (= x y)
               (:header navdata) 0x55667788
               (:seq-num navdata) 300711
               (:vision-flag navdata) true))

        (testing "state"
          (let [state (:state navdata)]
            (are [x y] (every? (fn [[k v]] (= (x k) v)) y)
                 state {:flying :landed}
                 state {:video :off}
                 state {:vision :off}
                 state {:altitude-control :on}
                 state {:command-ack :received}
                 state {:camera :ready}
                 state {:travelling :off}
                 state {:usb :not-ready}
                 state {:demo :off}
                 state {:bootstrap :off}
                 state {:motors :ok}
                 state {:communication :ok}
                 state {:software :ok}
                 state {:bootstrap :off}
                 state {:battery :ok}
                 state {:emergency-landing :off}
                 state {:timer :not-elapsed}
                 state {:magneto :ok}
                 state {:angles :ok}
                 state {:wind :ok}
                 state {:ultrasound :ok}
                 state {:cutout :ok}
                 state {:pic-version :ok}
                 state {:atcodec-thread :on}
                 state {:navdata-thread :on}
                 state {:video-thread :on}
                 state {:acquisition-thread :on}
                 state {:ctrl-watchdog :ok}
                 state {:adc-watchdog :ok}
                 state {:com-watchdog :problem}
                 state {:emergency-landing :off})))

        (test-adc-data-frame-option navdata)

        (testing "time option"
          (is (= (:time navdata) 362.979125)))

        (testing "raw-measures option"
          (let [raw-meas (:raw-measures navdata)]
            (is (= (:accelerometers raw-meas)
                   {:x 2040 :y 2036 :z 2528}))
            (is (= (:gyroscopes raw-meas)
                   {:x -23 :y 15 :z 0}))
            (is (= (:gyroscopes-110 raw-meas)
                   {:x 0 :y 0}))
            (is (= (:battery-millivolts raw-meas) 11686))
            (is (= (:us-echo raw-meas)
                   {:start 0 :end 0 :association 3758 :distance 0}))
            (is (= (:us-curve raw-meas)
                   {:time 21423 :value 0 :ref 120}))
            (is (= (:echo raw-meas) {:flag-ini 1 :num 1 :sum 3539193}))
            (is (= (:alt-temp-raw raw-meas) 243))
            (is (= (:gradient raw-meas) 41))))

        (testing "phys-measures option"
          (let [phys-meas (:phys-measures navdata)]
            (is (= (:temperature phys-meas)
                   {:accelerometer 45.309303283691406 :gyroscope 55738}))
            (is (= (:accelerometers phys-meas)
                   {:x 80.2970962524414
                    :y -33.318603515625
                    :z -942.5283203125}))
            (is (= (:gyroscopes phys-meas)
                   {:x -0.11236488074064255
                    :y 0.06872134655714035
                    :z 0.06200997903943062}))
            (is (= (:alim3v3 phys-meas) 0))
            (is (= (:vref-epson phys-meas) 0))
            (is (= (:vref-idg phys-meas) 0))))

        (testing "wifi option"
          (let [wifi (:wifi navdata)]
            (is (= (:link-quality wifi) 1.0))))

        (testing "altitude option"
          (let [alt (:altitude navdata)]
            (are [x y] (= x y)
                 (:vision alt) 243
                 (:velocity alt) 0.0
                 (:ref alt) 0
                 (:raw alt) 243
                 (:observer alt) {:acceleration 0.0
                                  :altitude 0.0
                                  :x {:x 0.0
                                      :y 0.0
                                      :z 0.0}
                                  :state 0}
                 (:estimated alt) {:vb {:x 0.0
                                        :y 0.0}
                                   :state 0})))
        (testing "demo option"
          (let [demo (:demo navdata)]
            (are [x y] (= x y)
                 (:control-state demo) :landed
                 (:battery-percentage demo) 50
                 (:theta demo) (float 2.974)
                 (:phi demo) (float 0.55)
                 (:psi demo) (float 1.933)
                 (:altitude demo) 0.0
                 (:velocity demo) {:x 0.0585307739675045
                                   :y -0.8817979097366333
                                   :z 0.0})))

        (testing "euler angles option"
          (let [euler (:euler-angles navdata)]
            (is (= (:theta euler) 4866.0))
            (is (= (:phi euler) 2024.0))))

        (testing "games option"
          (let [games (:games navdata)]
            (is (= games {:counters {:double-tap 0 :finish-line 0}}))))

        (test-gps-option navdata)

        (testing "gryos offsets option"
          (let [gyros (:gyros-offsets navdata)]
            (is (= gyros {:x -0.5329172611236572
                          :y 0.1788240224123001,
                          :z 0.0}))))

        (testing "magneto option"
          (let [magneto (:magneto navdata)]
            (are [x y] (= x y)
                 (:mx magneto) 30
                 (:my magneto) -56
                 (:mz magneto) 80
                 (:raw magneto) {:x 189.0 :y -100.8984375 :z -278.4375}
                 (:rectified magneto) {:x 145.08058166503906
                                       :y -84.93736267089844
                                       :z -287.18157958984375}
                 (:offset magneto) {:x 29.21237564086914
                                    :y -13.282999038696289
                                    :z 0.0}
                 (:heading magneto)  {:unwrapped (float 0.0)
                                      :gyro-unwrapped (float 4.132266E-4)
                                      :fusion-unwrapped (float 1.9333557)}
                 (:calibration-ok magneto) 1
                 (:state magneto) 2
                 (:radius magneto) (float 387.31146)
                 (:error magneto) {:mean (float -211.51361)
                                   :variance (float 79.36719)})))

        (testing "pressure raw option"
          (let [pressure-raw (:pressure-raw navdata)]
            (is (= pressure-raw
                   {:pressure 101586
                    :temperature 435
                    :ut 32556
                    :up 39148}))))

        (testing "pwm option"
          (let [pwm (:pwm navdata)]
            (are [x y] (= x y)
                 (:motors pwm) [0 0 0 0]
                 (:sat-motors pwm) [255 255 255 255]
                 (:gaz-feed-forward pwm) 0.0
                 (:gaz-altitude pwm) 0.0
                 (:altitude-integral pwm) 0.0
                 (:vz-ref pwm) 0.0
                 (:u-pitch pwm) 0
                 (:u-roll pwm) 0
                 (:u-yaw pwm) 0
                 (:yaw-u-i pwm) 0
                 (:u-pitch-planif pwm) 0
                 (:u-roll-planif pwm) 0
                 (:u-yaw-planif pwm) 0
                 (:u-gaz-planif pwm) 0
                 (:motor-currents pwm) [0 0 0 0]
                 (:altitude-prop pwm) 0.0
                 (:altitude-der pwm) 0.0)))

        (testing "rc references option"
          (let [rc-ref (:rc-references navdata)]
            (are [x y] (= x y)
                 (:pitch rc-ref) 0
                 (:roll rc-ref) 0
                 (:yaw rc-ref) 0
                 (:gaz rc-ref) 0
                 (:az rc-ref) 0)))

        (testing "references option"
          (let [ref (:references navdata)]
            (are [x y] (= x y)
                 (:theta ref) 0
                 (:phi ref) 0
                 (:psi ref) 0
                 (:theta-i ref) 0
                 (:phi-i ref) 0
                 (:pitch ref) 0
                 (:roll ref) 0
                 (:yaw ref) 0
                 (:psi ref) 0
                 (:vx ref) 0.0
                 (:vy ref) 0.0
                 (:theta-mod ref) 0.0
                 (:phi-mod ref) 0.0
                 (:k-v-x ref) 0.0
                 (:k-v-y ref) 0.0
                 (:k-mode ref) 0.0
                 (:ui ref) {:time 0.0
                            :theta 0.0
                            :phi 0.0
                            :psi 0.0
                            :psi-accuracy 0.0
                            :seq 0})))

        (test-trackers-send-option navdata)

        (testing "trims option"
          (let [trims (:trims navdata)]
            (is (= (:angular-rates trims) {:r 0.0}))
            (is (= (:euler-angles trims) {:theta (float 3028.916)
                                          :phi (float 1544.3184)}))))

        (testing "video-stream option"
          (let [video-stream (:video-stream navdata)]
            (is (= video-stream
                   {:at-cmd {:mean-gap 0 :quality 0 :sequence 0 :var-gap 0}
                    :bitrate {:desired 0 :out 0}
                    :data [0 0 0 0 0]
                    :fifo-queue-level 0
                    :frame {:number 46105 :size 4597}
                    :quant 0
                    :tcp-queue-level 0}))))

        (test-vision-option navdata)

        (testing "vision-detect option"
          (let [detections (:vision-detect navdata)]
            (is (= (count detections) 0))))

        (testing "vision-of option"
          (let [v (:vision-of navdata)]
            (is (= (:dx v) [0.0 0.0 0.0 0.0 0.0]))
            (is (= (:dy v) [0.0 0.0 0.0 0.0 0.0]))))

        (test-vision-perf-option navdata)

        (testing "vision-raw option"
          (let [v (:vision-raw navdata)]
            (is (= (:tx v) 1.3266397714614868))
            (is (= (:ty v) -0.7230937480926514))
            (is (= (:tz v) 0.0))))

        (test-watchdog-option navdata)

        (testing "windspeed option"
          (let [wind-speed (:wind-speed navdata)]
            (is (= wind-speed
                   {:angle (float 0.0)
                    :compensation {:phi (float 0.0) :theta (float 0.0)}
                    :debug [(float 0.0)
                            (float 0.0)
                            (float 0.0)]
                    :speed (float 0.0)
                    :state-x [(float 0.058451913)
                              (float -0.88172805)
                              (float 0.0)
                              (float 0.0)
                              (float 305.59628)
                              (float -236.80516)]}))))
        ))))


(deftest navdata-bytes-seq-tests
  (testing "navdata-bytes-seq on specimen"
    (let [navdata-bytes (xio/binary-slurp (io/resource "navdata.bin"))
          navdata-seq (navdata/navdata-bytes-seq navdata-bytes)]
      (is (= (count navdata-seq) 2))
      (let [navdata-bb (first navdata-seq)
            options-bbs (second navdata-seq)]
        (is (instance? ByteBuffer navdata-bb))
        (is (= (.remaining navdata-bb) 16))
        (is (= (count options-bbs) 29))
        (is (every? #(= (count %) 2) options-bbs))
        (is (every? #(or (keyword? %) (number? %)) (map first options-bbs)))
        (is (every? #(instance? ByteBuffer %) (map second options-bbs)))))))
