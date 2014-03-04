(ns com.lemondronor.turboshrimp.navdata-test
  (:require [clojure.java.io :as io]
            [clojure.pprint :as pprint]
            [clojure.test :refer :all]
            [com.lemonodor.xio :as xio]
            [midje.sweet :refer :all]
            [com.lemondronor.turboshrimp.navdata :refer :all]
            [com.lemondronor.turboshrimp :refer :all])
  (:import (java.net InetAddress DatagramSocket)))

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
(def b-target-option-id [16 0])
(def b-target-option-size [72 1])
(def b-target-num-tags-detected [2 0 0 0])
(def b-target-type [1 0 0 0 2 0 0 0 3 0 0 0 4 0 0 0])
(def b-target-xc [1 0 0 0 2 0 0 0 3 0 0 0 4 0 0 0])
(def b-target-yc [1 0 0 0 2 0 0 0 3 0 0 0 4 0 0 0])
(def b-target-width [1 0 0 0 2 0 0 0 3 0 0 0 4 0 0 0])
(def b-target-height [1 0 0 0 2 0 0 0 3 0 0 0 4 0 0 0])
(def b-target-dist [1 0 0 0 2 0 0 0 3 0 0 0 4 0 0 0])
(def b-target-orient-angle [0 96 -122 -60 0 96 -122 -60 0 96 -122 -60 0 96 -122 -60])
(def b-target-rotation (flatten (conj b-matrix33  b-matrix33  b-matrix33  b-matrix33)))
(def b-target-translation (flatten (conj b-vector31 b-vector31 b-vector31 b-vector31)))
(def b-target-camera-source [1 0 0 0 2 0 0 0 2 0 0 0 2 0 0 0])
(def b-target-option (flatten (conj b-target-option-id b-target-option-size
                                    b-target-num-tags-detected
                                    b-target-type b-target-xc b-target-yc
                                    b-target-width b-target-height b-target-dist
                                    b-target-orient-angle b-target-rotation b-target-translation
                                    b-target-camera-source)))

(def b-options (flatten (conj b-demo-option b-target-option)))
(def header (map byte [-120 119 102 85]))
(def nav-input  (map byte (flatten (conj b-header b-state b-seqnum b-vision b-demo-option b-target-option))))
(def host (InetAddress/getByName "192.168.1.1"))
(def port 5554)
(def socket (DatagramSocket. ))
(def packet (new-datagram-packet (byte-array 2048) host port))


(deftest navdata-tests
  (facts "about new-datagram-packet"
    (fact "getPort/getAddress/getData"
      (let [data (byte-array (map byte [1 0 0 0]))
            ndp (new-datagram-packet data host port)]
        (.getPort ndp) => port
        (.getAddress ndp) => host
        (.getData ndp) => data)))

  (facts "about get-int"
    (fact "get-int"
      (get-int (byte-array header) 0) => 0x55667788))

  (facts "about get-short"
    (fact "get-short"
      (get-short (map byte b-demo-option-size) 0) => 148))

  (facts "about get-float"
    (fact "get-float"
      (get-float (map byte b-demo-pitch) 0) => -1075.0))

  (facts "about get-int-by-n"
    (fact "get-int-by-n"
      (get-int-by-n (map byte b-target-type) 0 0) => 1
      (get-int-by-n (map byte b-target-type) 0 1) => 2
      (get-int-by-n (map byte b-target-type) 0 2) => 3
      (get-int-by-n (map byte b-target-type) 0 3) => 4))


  (facts "about get-float-by-n"
    (fact "get-float-by-n"
      (get-float-by-n (map byte b-demo-pitch) 0 0) => -1075.0))

  (facts "about parse-control-state"
    (fact "parse-control-state"
      (parse-control-state b-demo-option 4) => :landed))

  (facts "about parse-demo-option"
    (fact "parse-demo-option"
      (let [option (parse-demo-option b-demo-option 0)]
        option => (contains {:control-state :landed})
        option => (contains {:battery-percent 100 })
        option => (contains {:pitch (float -1.075) })
        option => (contains {:roll (float -2.904) })
        option => (contains {:yaw (float -0.215) })
        option => (contains {:altitude  (float 0.0) })
        option => (contains {:velocity-x  (float 0.0) })
        option => (contains {:velocity-y  (float 0.0) })
        option => (contains {:velocity-z  (float 0.0) })
        option => (contains {:detect-camera-type :roundel-under-drone }))))


  (facts "about parse-navdata"
    (fact "parse-navdata"
      (let [navdata (parse-navdata nav-input)]
        navdata => (contains {:header 0x55667788})
        navdata => (contains {:battery :ok})
        navdata => (contains {:flying :landed})
        navdata => (contains {:seq-num 870})
        navdata => (contains {:vision-flag false})
        navdata => (contains {:control-state :landed})
        navdata => (contains {:battery-percent 100 })
        navdata => (contains {:pitch (float -1.075) })
        navdata => (contains {:roll (float -2.904) })
        navdata => (contains {:yaw (float -0.215) })
        navdata => (contains {:altitude (float 0.0) })
        navdata => (contains {:velocity-x (float 0.0)})
        navdata => (contains {:velocity-y (float 0.0)})
        navdata => (contains {:velocity-z (float 0.0)}))
      (let [navdata (parse-navdata (xio/binary-slurp (io/resource "navdata.bin")))]
        navdata => (contains {:flying :landed})
        navdata => (contains {:video :off})
        navdata => (contains {:vision :off})
        navdata => (contains {:altitude-control :on})
        navdata => (contains {:command-ack :received})
        navdata => (contains {:camera :ready})
        navdata => (contains {:travelling :off})
        navdata => (contains {:usb :not-ready})
        navdata => (contains {:demo :off})
        navdata => (contains {:bootstrap :off})
        navdata => (contains {:motors :ok})
        navdata => (contains {:communication :ok})
        navdata => (contains {:software :ok})
        navdata => (contains {:bootstrap :off})
        navdata => (contains {:battery :ok})
        navdata => (contains {:emergency-landing :off})
        navdata => (contains {:timer :not-elapsed})
        navdata => (contains {:magneto :ok})
        navdata => (contains {:angles :ok})
        navdata => (contains {:wind :ok})
        navdata => (contains {:ultrasound :ok})
        navdata => (contains {:cutout :ok})
        navdata => (contains {:pic-version :ok})
        navdata => (contains {:atcodec-thread :on})
        navdata => (contains {:navdata-thread :on})
        navdata => (contains {:video-thread :on})
        navdata => (contains {:acquisition-thread :on})
        navdata => (contains {:ctrl-watchdog :ok})
        navdata => (contains {:adc-watchdog :ok})
        navdata => (contains {:com-watchdog :problem})
        navdata => (contains {:emergency-landing :off})
        navdata => (contains {:seq-num 300711})
        navdata => (contains {:vision-flag true}))))


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


  (facts "about parse-nav-state"
    (fact "parse-nav-state"
      (let [ state 260048080
            result (parse-nav-state state)
            {:keys [ flying video vision control altitude-control
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

  (facts "about which-option-type"
    (fact "which-option-type"
      (which-option-type 0) => :demo
      (which-option-type 16) => :target-detect
      (which-option-type 2342342) => :unknown))

  (facts "about parse-tag-detect"
    (fact "parse-tag-detect"
      (parse-tag-detect 131072) => :vertical-hsync))

  (facts "about parse-target-tag"
    (fact "about parse-target-tag with the first target"
      (let [tag (parse-target-tag (map byte b-target-option) 0 0)]
        tag => (contains {:target-type :horizontal})
        tag => (contains {:target-xc 1})
        tag => (contains {:target-yc 1})
        tag => (contains {:target-width 1})
        tag => (contains {:target-height 1})
        tag => (contains {:target-dist 1})
        tag => (contains {:target-orient-angle -1075.0})
        tag => (contains {:target-camera-source :vertical})))
    (fact "about parse-target-tag with the second target"
      (let [tag (parse-target-tag (map byte b-target-option) 0 1)]
        tag => (contains {:target-type :horizontal})
        tag => (contains {:target-xc 2})
        tag => (contains {:target-yc 2})
        tag => (contains {:target-width 2})
        tag => (contains {:target-height 2})
        tag => (contains {:target-dist 2})
        tag => (contains {:target-orient-angle -1075.0})
        tag => (contains {:target-camera-source :vertical-hsync})))
    (fact "about parse-target-tag with the third target"
      (let [tag (parse-target-tag (map byte b-target-option) 0 2)]
        tag => (contains {:target-type :horizontal})
        tag => (contains {:target-xc 3})
        tag => (contains {:target-yc 3})
        tag => (contains {:target-width 3})
        tag => (contains {:target-height 3})
        tag => (contains {:target-dist 3})
        tag => (contains {:target-orient-angle -1075.0})
        tag => (contains {:target-camera-source :vertical-hsync})))
    (fact "about parse-target-tag with the fourth target"
      (let [tag (parse-target-tag (map byte b-target-option) 0 3)]
        tag => (contains {:target-type :horizontal})
        tag => (contains {:target-xc 4})
        tag => (contains {:target-yc 4})
        tag => (contains {:target-width 4})
        tag => (contains {:target-height 4})
        tag => (contains {:target-dist 4})
        tag => (contains {:target-orient-angle -1075.0})
        tag => (contains {:target-camera-source :vertical-hsync}))))

  (facts "about parse-target-option"
    (fact "parse-target-option"
      (let [t-tag {:target-type :horizontal
                   :target-xc 1
                   :target-yc 1
                   :target-width 1
                   :target-height 1
                   :target-dist 1
                   :target-orient-angle -1075.0
                   :target-camera-source 1}
            option (parse-target-option b-target-option 0)
            targets (:targets option)]
        option => (contains {:targets-num 2})
        (count targets) => 2
        (first targets) => (contains {:target-type :horizontal}))))

  (facts "about parse-options"
    (fact "about parse-options with demo"
      (let [option (parse-options b-demo-option 0 {})]
        option => (contains {:control-state :landed})))
    (fact "about parse option with targets"
      (let [option (parse-options b-target-option 0 {})]
        option => (contains {:targets-num 2})))
    (fact "about parse-options with demo and targets"
      (let [options (parse-options nav-input 16 {})]
        options => (contains {:control-state :landed})
        options => (contains {:targets-num 2})))))
