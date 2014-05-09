(ns com.lemondronor.turboshrimp-test
  (:import (java.net DatagramPacket InetAddress))
  (:require [clojure.test :refer :all]
            [com.lemondronor.turboshrimp :as ar-drone]
            [echo.test.mock :as mock]))


(deftest turboshrimp-tests
  (testing "default initialize gets default host and port"
    (let [drone (ar-drone/make-drone)]
      (is (= (.getHostName (:host drone)) ar-drone/default-hostname))
      (is (= (:at-port drone) ar-drone/default-at-port))
      (is (= @(:counter drone) 0))))

  (testing "custom initialize uses custom name host and port"
    (let [drone (ar-drone/make-drone
                 :name :frank
                 :hostname "192.168.2.2"
                 :at-port 4444)]
      (is (= (.getHostName (:host drone)) "192.168.2.2"))
      (is (= (:at-port drone) 4444))
      (is (= @(:counter drone) 0))))

  (testing "command passes along the data to send-at-command"
    (let [drone (ar-drone/make-drone)]
      (mock/expect
       [ar-drone/send-at-command
        (->>
         (mock/has-args [drone "AT*FTRIM=1,\r"])
         (mock/times 1))]
       (ar-drone/connect! drone))
      (mock/expect
        [ar-drone/send-at-command
        (->>
         (mock/has-args [drone "AT*REF=2,290718208\r"])
         (mock/times 1))]
       (ar-drone/command drone :take-off))))

  (testing "drone-do-for command calls drone command every 30 sec"
    (let [drone (ar-drone/make-drone)]
      (mock/expect
       [ar-drone/command
        (->>
         (mock/has-args [drone :take-off nil nil nil nil])
         (mock/times #(< 0 %)))]
       (ar-drone/drone-do-for drone 1 :take-off)))))
