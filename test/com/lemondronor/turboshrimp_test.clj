(ns com.lemondronor.turboshrimp-test
  (:require [clojure.test :refer :all]
            [com.lemondronor.turboshrimp :as ar-drone]
            [echo.test.mock :as mock]))


(deftest turboshrimp-tests
  (testing "print-method"
    (is (re-find #"Drone" (str (ar-drone/make-drone)))))
  (testing "default initialize gets default host and port"
    (let [drone (ar-drone/make-drone)]
      (is (= ar-drone/default-hostname (:hostname drone)))
      (is (= ar-drone/default-at-port (:port drone)))))
  (testing "custom initialize uses custom name host and port"
    (let [drone (ar-drone/make-drone
                 :name :frank
                 :hostname "192.168.2.2"
                 :at-port 4444)]
      (is (= "192.168.2.2" (:hostname drone)))
      (is (= 4444 (:port drone)))))
  ;; (testing "command passes along the data to send-at-command"
  ;;   (let [drone (ar-drone/make-drone)]
  ;;     (mock/expect
  ;;      [ar-drone/send-at-command
  ;;       (->>
  ;;        (mock/has-args [drone "AT*FTRIM=1,\r"])
  ;;        (mock/times 1))]
  ;;      (ar-drone/connect! drone))
  ;;     (mock/expect
  ;;       [ar-drone/send-at-command
  ;;       (->>
  ;;        (mock/has-args [drone "AT*REF=2,290718208\r"])
  ;;        (mock/times 1))]
  ;;      (ar-drone/command drone :take-off))))
  (testing "drone-do-for command calls drone command every 30 sec"
    (let [drone (ar-drone/make-drone)]
      (mock/expect
       [ar-drone/command
        (->>
         (mock/has-args [drone :take-off nil nil nil nil])
         (mock/times #(< 0 %)))]
       (ar-drone/drone-do-for drone 1 :take-off)))))
