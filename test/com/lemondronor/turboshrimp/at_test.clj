(ns com.lemondronor.turboshrimp.at-test
  (:require [clojure.test :refer :all]
            [com.lemondronor.turboshrimp.at :as at]))


(deftest at-tests
  (testing "bit command vectors are translated into ints"
    (is (= (at/build-command-int [18 20 22 24 28]) 290717696))
    (is (= (at/build-command-int [9 18 20 22 24 28]) 290718208)))

  (testing "floats are cast to int"
    (is (= (int -1085485875)  (at/cast-float-to-int (float -0.8))))
    (is (= (int -1085485875) (at/cast-float-to-int (double -0.8)))))

  (testing "commands are build correctly"
    (are [x y] (= x y)
      (at/build-command 1 :take-off) "AT*REF=1,290718208\r"
      (at/build-command 2 :land) "AT*REF=2,290717696\r"
      (at/build-command 3 :spin-right 0.5) "AT*PCMD=3,1,0,0,0,1056964608\r"
      (at/build-command 3 :spin-left 0.8) "AT*PCMD=3,1,0,0,0,-1085485875\r"
      (at/build-command 3 :up 0.5) "AT*PCMD=3,1,0,0,1056964608,0\r"
      (at/build-command 3 :down 0.8) "AT*PCMD=3,1,0,0,-1085485875,0\r"
      (at/build-command 3 :tilt-back 0.5) "AT*PCMD=3,1,0,1056964608,0,0\r"
      (at/build-command 3 :tilt-front 0.8) "AT*PCMD=3,1,0,-1085485875,0,0\r"
      (at/build-command 3 :tilt-right 0.5) "AT*PCMD=3,1,1056964608,0,0,0\r"
      (at/build-command 3 :tilt-left 0.8) "AT*PCMD=3,1,-1085485875,0,0,0\r"
      (at/build-command 3 :hover) "AT*PCMD=3,0,0,0,0,0\r"
      (at/build-command 3 :fly 0.5 -0.8 0.5 -0.8)
      "AT*PCMD=3,1,1056964608,-1085485875,1056964608,-1085485875\r"
      (at/build-command 3 :fly 0 0 0 0.5) (at/build-command 3 :spin-right 0.5)
      (at/build-command 3 :flat-trim) "AT*FTRIM=3,\r"
      (at/build-command 3 :reset-watchdog) "AT*COMWDG=3,\r"
      (at/build-command 3 :init-navdata)
      "AT*CONFIG=3,\"general:navdata_demo\",\"FALSE\"\r"
      (at/build-command 3 :control-ack) "AT*CTRL=3,0\r"
      (at/build-command 3 :init-targeting)
      "AT*CONFIG=3,\"detect:detect_type\",\"10\"\r"
      (at/build-command 3 :target-shell-h)
      "AT*CONFIG=3,\"detect:detections_select_h\",\"32\"\r"
      (at/build-command 3 :target-roundel-v)
      "AT*CONFIG=3,\"detect:detections_select_v_hsync\",\"128\"\r"
      (at/build-command 3 :target-color-green)
      "AT*CONFIG=3,\"detect:enemy_colors\",\"1\"\r"
      (at/build-command 3 :target-color-yellow)
      "AT*CONFIG=3,\"detect:enemy_colors\",\"2\"\r"
      (at/build-command 3 :target-color-blue)
      "AT*CONFIG=3,\"detect:enemy_colors\",\"3\"\r"))

  (testing "commands-bytes"
    (is (= "AT*FTRIM=0,\r" (String. (at/commands-bytes 0 '((:flat-trim))))))
    (is (= "AT*FTRIM=0,\rAT*REF=1,290718208\r"
           (String. (at/commands-bytes 0 '((:flat-trim) (:take-off))))))
    (is (= (str "AT*PCMD=3,1,-1085485875,0,0,0\r"
                "AT*PCMD=4,1,0,0,1056964608,0\r")
           (String. (at/commands-bytes 3 '((:tilt-left 0.8) (:up 0.5))))))))
