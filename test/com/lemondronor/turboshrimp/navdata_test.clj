(ns com.lemondronor.turboshrimp.navdata-test
  (:require [clojure.java.io :as io]
            [clojure.pprint :as pprint]
            [clojure.test :refer :all]
            [com.lemonodor.xio :as xio]
            [criterium.core :as criterium]
            [gloss.io]
            [midje.sweet :refer :all]
            [com.lemondronor.turboshrimp.navdata :refer :all]
            [com.lemondronor.turboshrimp :refer :all])
  (:import (java.net InetAddress DatagramSocket)
           (java.nio ByteOrder)))

;; matrix 33 is 9 floats
;; vector 31 is 3 floats
(def b-matrix33  (vec (repeat (* 9 4) 0 )))
(def b-vector31  (vec (repeat (* 3 4) 0 )))
(* 12 4)

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

(def b-options (flatten (conj b-demo-option b-vision-detect-option)))
(def header (map byte [-120 119 102 85]))
(def nav-input  (byte-array (map byte (flatten (conj b-header b-state b-seqnum b-vision b-demo-option b-vision-detect-option)))))
(def host (InetAddress/getByName "192.168.1.1"))
(def port 5554)
(def socket (DatagramSocket. ))
(def packet (new-datagram-packet (byte-array 2048) host port))


(deftest navdata-unit-tests
  (facts "about new-datagram-packet"
    (fact "getPort/getAddress/getData"
      (let [data (byte-array (map byte [1 0 0 0]))
            ndp (new-datagram-packet data host port)]
        (.getPort ndp) => port
        (.getAddress ndp) => host
        (.getData ndp) => data)))

  (facts "about parse-nav-state"
    (fact "parse-nav-state"
      (let [state 260048080
            result (parse-nav-state state)
            {:keys [flying video vision control altitude-control
                    user-feedback command-ack camera travelling
                    usb demo bootstrap motors communication
                    software battery emergency-landing timer
                    magneto angles wind ultrasound cutout
                    pic-version atcodec-thread navdata-thread
                    video-thread acquisition-thread ctrl-watchdog
                    adc-watchdog com-watchdog emergency]} result]
        flying => :landed
        video => :off
        vision => :off
        control => :euler-angles
        altitude-control => :on
        user-feedback => :off
        command-ack => :received
        camera => :ready
        travelling => :off
        usb => :not-ready
        demo => :on
        bootstrap => :off
        motors => :ok
        communication => :ok
        software => :ok
        battery => :ok
        emergency-landing => :off
        timer => :not-elapsed
        magneto => :ok
        angles => :ok
        wind => :ok
        ultrasound => :ok
        cutout => :ok
        pic-version => :ok
        atcodec-thread => :on
        navdata-thread => :on
        video-thread => :on
        acquisition-thread => :on
        ctrl-watchdog => :ok
        adc-watchdog => :ok
        com-watchdog => :ok
        emergency => :ok)))

  (fact "which-option-type"
    (which-option-type 0) => :demo
    (which-option-type 16) => :vision-detect
    (which-option-type 2342342) => nil)

  (facts "about parse-control-state"
    (fact "parse-control-state"
      (let [bb (doto (gloss.io/to-byte-buffer b-demo-control-state)
                            (.order ByteOrder/LITTLE_ENDIAN))
            control-state (.getInt bb)]
        (parse-control-state control-state) => :landed)))

  (facts "about parse-demo-option"
    (fact "parse-demo-option"
      (let [bb (doto (gloss.io/to-byte-buffer
                      ;; Skip past the option ID and option size.
                      (drop 4 b-demo-option))
                 (.order ByteOrder/LITTLE_ENDIAN))
            option (parse-demo-option bb)]
        (:control-state option) => :landed
        (:battery-percentage option) => 100
        option => (contains {:theta (float -1.075) })
        option => (contains {:phi (float -2.904) })
        option => (contains {:psi (float -0.215) })
        option => (contains {:altitude (float 0.0) })
        option => (contains {:velocity {:x (float 0.0)
                                        :y (float 0.0)
                                        :z (float 0.0)}})
        option => (contains {:detect-camera-type :roundel-under-drone }))))

  (facts "about parse-vision-detect-option"
    (let [detections
          (parse-vision-detect-option
           (doto (gloss.io/to-byte-buffer
                  ;; Skip past the option ID and option size.
                  (drop 4
                        b-vision-detect-option))
             (.order ByteOrder/LITTLE_ENDIAN)))]
      (count detections) => 2
      (fact "first detection"
        (let [det (nth detections 0)]
          (:type det) => :vertical-deprecated
          (:xc det) => 1
          (:yc det) => 1
          (:width det) => 1
          (:height det) => 1
          (:dist det) => 1
          (:orientation-angle det) => -1075.0
          (:camera-source det) => :vertical
          (:translation det) => {:x 0.0 :y 0.0 :z 0.0}
          (:rotation det) => {:m11 0.0, :m12 0.0, :m13 0.0,
                              :m21 0.0, :m22 0.0, :m23 0.0,
                              :m31 0.0, :m32 0.0, :m33 0.0}))
      (fact "second detection"
        (let [det (nth detections 1)]
          (:type det) => :horizontal-drone-shell
          (:xc det) => 2
          (:yc det) => 2
          (:width det) => 2
          (:height det) => 2
          (:dist det) => 2
          (:orientation-angle det) => -1075.0
          (:camera-source det) => :vertical-hsync
          (:translation det) => {:x 0.0 :y 0.0 :z 0.0}
          (:rotation det) => {:m11 0.0, :m12 0.0, :m13 0.0,
                              :m21 0.0, :m22 0.0, :m23 0.0,
                              :m31 0.0, :m32 0.0, :m33 0.0}))))

  (facts "about parse-navdata"
    (fact "parse-navdata"
      (fact "hand-crafted input"
        (let [navdata (parse-navdata nav-input)]
          navdata => (contains {:header 0x55667788})
          navdata => (contains {:seq-num 870})
          navdata => (contains {:vision-flag false})
          (fact "state"
            (let [state (:state navdata)]
              state => (contains {:battery :ok})
              state => (contains {:flying :landed})))
          (fact "demo"
            (let [demo (:demo navdata)]
              (:control-state demo) => :landed
              (:battery-percentage demo) => 100
              demo => (contains {:theta (float -1.075) })
              demo => (contains {:phi (float -2.904) })
              demo => (contains {:psi (float -0.215) })
              demo => (contains {:altitude (float 0.0) })
              demo => (contains {:velocity
                                 {:x (float 0.0)
                                  :y (float 0.0)
                                  :z (float 0.0)}}))))))))


(deftest navdata-specimen-tests
  (facts "parse-navdata on specimen"
    (let [navdata-bytes (xio/binary-slurp (io/resource "navdata.bin"))]
      ;;(println "Benchmarking parse-navdata")
      ;;(criterium/bench (parse-navdata navdata-bytes))
      ;;(criterium/bench (:gps (parse-navdata navdata-bytes)))
      (let [navdata (parse-navdata navdata-bytes)]

        (fact "navdata"
          (:header navdata) => 0x55667788
          (:seq-num navdata) => 300711
          (:vision-flag navdata) => true)

        (fact "state"
          (let [state (:state navdata)]
            state => (contains {:flying :landed})
            state => (contains {:video :off})
            state => (contains {:vision :off})
            state => (contains {:altitude-control :on})
            state => (contains {:command-ack :received})
            state => (contains {:camera :ready})
            state => (contains {:travelling :off})
            state => (contains {:usb :not-ready})
            state => (contains {:demo :off})
            state => (contains {:bootstrap :off})
            state => (contains {:motors :ok})
            state => (contains {:communication :ok})
            state => (contains {:software :ok})
            state => (contains {:bootstrap :off})
            state => (contains {:battery :ok})
            state => (contains {:emergency-landing :off})
            state => (contains {:timer :not-elapsed})
            state => (contains {:magneto :ok})
            state => (contains {:angles :ok})
            state => (contains {:wind :ok})
            state => (contains {:ultrasound :ok})
            state => (contains {:cutout :ok})
            state => (contains {:pic-version :ok})
            state => (contains {:atcodec-thread :on})
            state => (contains {:navdata-thread :on})
            state => (contains {:video-thread :on})
            state => (contains {:acquisition-thread :on})
            state => (contains {:ctrl-watchdog :ok})
            state => (contains {:adc-watchdog :ok})
            state => (contains {:com-watchdog :problem})
            state => (contains {:emergency-landing :off})))

        (fact "time option"
          (:time navdata) => 362.979125)

        (fact "raw-measures option"
          (let [raw-meas (:raw-measures navdata)]
            (:accelerometers raw-meas) => {:x 2040
                                           :y 2036
                                           :z 2528}
            (:gyroscopes raw-meas) => {:x -23
                                       :y 15
                                       :z 0}
            (:gyroscopes-110 raw-meas) => {:x 0
                                           :y 0}
            (:battery-millivolts raw-meas) => 11686
            (:us-echo raw-meas) => {:start 0
                                    :end 0
                                    :association 3758
                                    :distance 0}
            (:us-curve raw-meas) => {:time 21423
                                     :value 0
                                     :ref 120}
            (:echo raw-meas) => {:flag-ini 1
                                 :num 1
                                 :sum 3539193}
            (:alt-temp-raw raw-meas) => 243
            (:gradient raw-meas) => 41))

        (fact "phys-measures option"
          (let [phys-meas (:phys-measures navdata)]
            (:temperature phys-meas) => {:accelerometer 45.309303283691406
                                         :gyroscope 55738}
            (:accelerometers phys-meas) => {:x 80.2970962524414
                                            :y -33.318603515625
                                            :z -942.5283203125}
            (:gyroscopes phys-meas) => {:x -0.11236488074064255
                                        :y 0.06872134655714035
                                        :z 0.06200997903943062}
            (:alim3v3 phys-meas) => 0
            (:vref-epson phys-meas) => 0
            (:vref-idg phys-meas) => 0))

        (fact "wifi option"
          (let [wifi (:wifi navdata)]
            (:link-quality wifi) => 1.0))

        (fact "demo option"
          (let [demo (:demo navdata)]
            (:control-state demo) => :landed
            (:battery-percentage demo) => 50
            (:theta demo) => (float 2.974)
            (:phi demo) => (float 0.55)
            (:psi demo) => (float 1.933)
            (:altitude demo) => 0.0
            (:velocity demo) => {:x 0.0585307739675045
                                 :y -0.8817979097366333
                                 :z 0.0}))

        (fact "euler angles option"
          (let [euler (:euler-angles navdata)]
            (:theta euler) => 4866.0
            (:phi euler) => 2024.0))

        (fact "gps option"
          (let [gps (:gps navdata)]
            (:latitude gps) => 34.0903478
            ;;(:longitude gps) => 0
            (:elevation gps) => 130.39
            (:lat0 gps) => 34.090359093568644
            (:lon0 gps) => -118.276604
            (:lat-fuse gps) => 34.09035909403431
            (:lon-fuse gps) => -118.276604
            (:pdop gps) => 0.0
            (:speed gps) => 0.4399999976158142
            (:last-frame-timestamp gps) => 1816.647945
            (:degree gps) => 170.16000366210938
            (:degree-mag gps) => 0.0
            (:channels gps) => [{:sat 22 :cn0 36}
                                {:sat 15 :cn0 17}
                                {:sat 11 :cn0 227}
                                {:sat 11 :cn0 227}
                                {:sat 18 :cn0 27}
                                {:sat 29 :cn0 16}
                                {:sat 21 :cn0 22}
                                {:sat 16 :cn0 0}
                                {:sat 27 :cn0 0}
                                {:sat 30 :cn0 0}
                                {:sat 12 :cn0 227}
                                {:sat 12 :cn0 227}]
            (:gps-plugged gps) => 1
            (:gps-time gps) => 0.0
            (:week gps) => 0
            (:gps-fix gps) => 0
            (:num-satellites gps) => 0))

        (fact "gryos offsets option"
          (let [gyros (:gyros-offsets navdata)]
            gyros => {:x -0.5329172611236572
                      :y 0.1788240224123001,
                      :z 0.0}))

        (fact "magneto option"
          (let [magneto (:magneto navdata)]
            (:mx magneto) => 30
            (:my magneto) => -56
            (:mz magneto) => 80
            (:raw magneto) => {:x 189.0 :y -100.8984375 :z -278.4375}
            (:rectified magneto) => {:x 145.08058166503906
                                     :y -84.93736267089844
                                     :z -287.18157958984375}
            (:offset magneto) => {:x 29.21237564086914
                                  :y -13.282999038696289
                                  :z 0.0}
            (:heading magneto) =>  {:unwrapped (float 0.0)
                                    :gyro-unwrapped (float 4.132266E-4)
                                    :fusion-unwrapped (float 1.9333557)}
            (:calibration-ok magneto) => 1
            (:state magneto) => 2
            (:radius magneto) => (float 387.31146)
            (:error magneto) => {:mean (float -211.51361)
                                 :variance (float 79.36719)}))

        (fact "pwm option"
          (let [pwm (:pwm navdata)]
            (:motors pwm) => [0 0 0 0]
            (:sat-motors pwm) => [255 255 255 255]
            (:gaz-feed-forward pwm) => 0.0
            (:gaz-altitude pwm) => 0.0
            (:altitude-integral pwm) => 0.0
            (:vz-ref pwm) => 0.0
            (:u-pitch pwm) => 0
            (:u-roll pwm) => 0
            (:u-yaw pwm) => 0
            (:yaw-u-i pwm) => 0
            (:u-pitch-planif pwm) => 0
            (:u-roll-planif pwm) => 0
            (:u-yaw-planif pwm) => 0
            (:u-gaz-planif pwm) => 0
            (:motor-currents pwm) => [0 0 0 0]
            (:altitude-prop pwm) => 0.0
            (:altitude-der pwm) => 0.0))

        (fact "rc references option"
          (let [rc-ref (:rc-references navdata)]
            (:pitch rc-ref) => 0
            (:roll rc-ref) => 0
            (:yaw rc-ref) => 0
            (:gaz rc-ref) => 0
            (:az rc-ref) => 0))

        (fact "references option"
          (let [ref (:references navdata)]
            (:theta ref) => 0
            (:phi ref) => 0
            (:psi ref) => 0
            (:theta-i ref) => 0
            (:phi-i ref) => 0
            (:pitch ref) => 0
            (:roll ref) => 0
            (:yaw ref) => 0
            (:psi ref) => 0
            (:vx ref) => 0.0
            (:vy ref) => 0.0
            (:theta-mod ref) => 0.0
            (:phi-mod ref) => 0.0
            (:k-v-x ref) => 0.0
            (:k-v-y ref) => 0.0
            (:k-mode ref) => 0.0
            (:ui ref) => {:time 0.0
                          :theta 0.0
                          :phi 0.0
                          :psi 0.0
                          :psi-accuracy 0.0
                          :seq 0}))

        (fact "trims option"
          (let [trims (:trims navdata)]
            (:angular-rates trims) => {:r 0.0}
            (:euler-angles trims) => {:theta (float 3028.916)
                                      :phi (float 1544.3184)}))

        (fact "vision-detect option"
          (let [detections (:vision-detect navdata)]
            (count detections) => 0))))))


(deftest stream-navdata-tests
  (facts "about stream-navdata"
    (fact "stream-navdata"
      (stream-navdata nil socket packet) => anything
      (provided
        (receive-navdata anything anything) => 1
        (get-nav-data :default) => (:nav-data (:default @drones))
        (get-navdata-bytes anything) => nav-input
        (get-ip-from-packet anything) => "192.168.1.1")
      (against-background
        (before :facts (do
                         (reset! drones {:default {:nav-data (atom {})
                                                   :host (InetAddress/getByName "192.168.1.1")
                                                   :current-belief (atom "None")
                                                   :current-goal (atom "None")
                                                   :current-goal-list (atom [])}})
                         (reset! stop-navstream true))))))
  )

  ;; (facts "about parse-options"
  ;;   (fact "about parse-options with demo"
  ;;     (let [option (parse-options b-demo-option 0 {})]
  ;;       option => (contains {:control-state :landed})))
  ;;   (fact "about parse option with targets"
  ;;     (let [option (parse-options b-target-option 0 {})]
  ;;       option => (contains {:targets-num 2})))
  ;;   (fact "about parse-options with demo and targets"
  ;;     (let [options (parse-options nav-input 16 {})]
  ;;       options => (contains {:control-state :landed})
  ;;       options => (contains {:targets-num 2}))))
