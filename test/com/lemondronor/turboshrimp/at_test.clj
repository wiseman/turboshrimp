(ns com.lemondronor.turboshrimp.at-test
  (:require [clojure.test :refer :all]
            [com.lemondronor.turboshrimp.at :as at]))


(deftest at-tests
  (testing "at-encoded-float"
    (is (= 1056964608 (at/at-encoded-float 0.5)))
    (is (= -1085485875 (at/at-encoded-float -0.8)))
    (is (= 0 (at/at-encoded-float 0))))
  (testing "ctrl"
    (is (= (at/map->AtCommand
            {:type "CTRL"
             :args [5 0]
             :blocks? false
             :options nil
             :callback nil})
           (at/build-command :ctrl 5 0)))
    (is (thrown-with-msg? clojure.lang.ExceptionInfo #"Wrong number of arguments"
                          (at/build-command :ctrl))))
  (testing "ref"
    (is (= (at/map->AtCommand
            {:type "REF",
             :args [256],
             :blocks? false,
             :options nil,
             :callback nil})
           (at/build-command :ref {:emergency true})))
    (is (= "AT*REF=7,256\r"
           (at/serialize 7 (at/build-command :ref {:emergency true}))))
    (is (= "AT*REF=7,0\r"
           (at/serialize 7 (at/build-command :ref {}))))
    (is (= "AT*REF=7,512\r"
           (at/serialize 7 (at/build-command :ref {:fly true}))))
    (is (= "AT*REF=7,0\r"
           (at/serialize 7 (at/build-command :ref {:fly false})))))
  (testing "pcmd"
    (is (= (at/map->AtCommand
            {:type "PCMD",
             :args [1 0 0 1056964608 0],
             :blocks? false,
             :options nil,
             :callback nil})
           (at/build-command :pcmd {:up 0.5})))
    (is (= "AT*PCMD=7,1,0,0,1056964608,0\r"
           (at/serialize 7 (at/build-command :pcmd {:up 0.5}))))
    (is (= "AT*PCMD=7,1,0,0,0,-1085485875\r"
           (at/serialize
            7 (at/build-command :pcmd {:counter-clockwise 0.8}))))
    (is (= "AT*PCMD=7,1,-1110651699,0,0,1050253722\r"
           (at/serialize
            7 (at/build-command :pcmd {:left 0.1 :clockwise 0.3}))))
    (is (zero? (bit-and (get-in (at/build-command :pcmd {}) [:args 0])
                        (:progressive at/pcmd-flags))))
    (is (not (zero? (bit-and (get-in (at/build-command :pcmd {:left 0.1})
                                     [:args 0])
                        (:progressive at/pcmd-flags))))))
  (testing "calibrate"
    (is (= "AT*CALIB=7,0\r"
           (at/serialize
            7 (at/build-command :calibrate 0))))
    (is (= "AT*CALIB=7,1\r"
           (at/serialize
            7 (at/build-command :calibrate 1)))))
  (testing "flat-trim"
    (is (= "AT*FTRIM=7\r"
           (at/serialize
            7 (at/build-command :flat-trim)))))
  (testing "animate-leds"
    (is (= "AT*CONFIG=8,\"leds:leds_anim\",\"9,1073741824,3\"\r"
           (at/serialize
            8 (at/build-command :animate-leds))))
    (is (= "AT*CONFIG=8,\"leds:leds_anim\",\"1,1073741824,3\"\r"
           (at/serialize
            8 (at/build-command :animate-leds :blink-green))))
    (is (= "AT*CONFIG=8,\"leds:leds_anim\",\"1,1077936128,3\"\r"
           (at/serialize
            8 (at/build-command :animate-leds :blink-green 3 3)))))
  (testing "animate"
    (is (= "AT*CONFIG=7,\"control:flight_anim\",\"8,2000\"\r"
           (at/serialize
            7 (at/build-command :animate :yaw-shake 2000)))))
  (testing "config"
    (is (= (at/map->AtCommand
            {:type "CONFIG"
             :args ["\"foo\"" "\"bar\""]
             :blocks? true
             :callback nil})
           (at/build-command :config "foo" "bar")))
    (is (= (at/map->AtCommand
            {:type "CONFIG"
             :args ["\"foo\"" "\"bar\""]
             :blocks? true
             :callback :bogus-callback})
           (at/build-command :config "foo" "bar" :bogus-callback)))))
