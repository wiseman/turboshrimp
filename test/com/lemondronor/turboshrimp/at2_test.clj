(ns com.lemondronor.turboshrimp.at2-test
  (:require
   [clojure.test :refer :all]
   [com.lemondronor.turboshrimp.at2 :as at2]))


(deftest at2-tests
  (testing "at-encoded-float"
    (is (= 1056964608 (at2/at-encoded-float 0.5)))
    (is (= -1085485875 (at2/at-encoded-float -0.8)))
    (is (= 0 (at2/at-encoded-float 0))))

  (testing "ctrl"
    (is (= (at2/map->AtCommand
            {:type "CTRL"
             :args [5 0]
             :blocks? false
             :options nil
             :callback nil}))))
  (testing "ref"
    (is (= (at2/map->AtCommand
            {:type "REF",
             :args [256],
             :blocks? false,
             :options nil,
             :callback nil})
           (at2/build-command :ref {:emergency true})))
    (is (= "AT*REF=7,256\r"
           (at2/serialize 7 (at2/build-command :ref {:emergency true}))))
    (is (= "AT*REF=7,0\r"
           (at2/serialize 7 (at2/build-command :ref {}))))
    (is (= "AT*REF=7,512\r"
           (at2/serialize 7 (at2/build-command :ref {:fly true}))))
    (is (= "AT*REF=7,0\r"
           (at2/serialize 7 (at2/build-command :ref {:fly false})))))
  (testing "pcmd"
    (is (= (at2/map->AtCommand
            {:type "PCMD",
             :args [1 0 0 1056964608 0],
             :blocks? false,
             :options nil,
             :callback nil})
           (at2/build-command :pcmd {:up 0.5})))
    (is (= "AT*PCMD=7,1,0,0,1056964608,0\r"
           (at2/serialize 7 (at2/build-command :pcmd {:up 0.5}))))
    (is (= "AT*PCMD=7,1,0,0,0,-1085485875\r"
           (at2/serialize
            7 (at2/build-command :pcmd {:counter-clockwise 0.8}))))
    (is (= "AT*PCMD=7,1,-1110651699,0,0,1050253722\r"
           (at2/serialize
            7 (at2/build-command :pcmd {:left 0.1 :clockwise 0.3}))))
    (is (zero? (bit-and (get-in (at2/build-command :pcmd {}) [:args 0])
                        (:progressive at2/pcmd-flags))))
    (is (not (zero? (bit-and (get-in (at2/build-command :pcmd {:left 0.1})
                                     [:args 0])
                        (:progressive at2/pcmd-flags))))))
  (testing "calibrate"
    (is (= "AT*CALIB=7,0\r"
           (at2/serialize
            7 (at2/build-command :calibrate 0))))
    (is (= "AT*CALIB=7,1\r"
           (at2/serialize
            7 (at2/build-command :calibrate 1)))))
  (testing "flat-trim"
    (is (= "AT*FTRIM=7\r"
           (at2/serialize
            7 (at2/build-command :flat-trim)))))
  (testing "animate-leds"
    (is (= "AT*CONFIG=8,\"leds:leds_anim\",\"1,1077936128,3\"\r"
           (at2/serialize
            8 (at2/build-command :animate-leds :blink-green 3 3)))))
  (testing "animate"
    (is (= "AT*CONFIG=7,\"control:flight_anim\",\"8,2000\"\r"
           (at2/serialize
            7 (at2/build-command :animate :yaw-shake 2000)))))
  (testing "config"
    (is (= (at2/map->AtCommand
            {:type "CONFIG"
             :args ["\"foo\"" "\"bar\""]
             :blocks? true
             :callback nil})
           (at2/build-command :config "foo" "bar" nil)))))
