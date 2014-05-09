(ns com.lemondronor.turboshrimp-test
  (:import (java.net DatagramPacket InetAddress))
  (:require [clojure.test :refer :all]
            [com.lemondronor.turboshrimp :as ar-drone]))


(deftest core-tests
  (testing "default initialize gets default host and port"
    (let [drone (ar-drone/make-drone)]
      (is (= (.getHostName (:host drone)) ar-drone/default-hostname))
      (is (= (:at-port drone) ar-drone/default-at-port))
      (is (= @(:counter drone) 0))))

  (testing "custom initiliaze uses custom name host and port"
    (let [drone (ar-drone/make-drone
                 :name :frank
                 :hostname "192.168.2.2"
                 :at-port 4444)]
      (is (= (.getHostName (:host drone)) "192.168.2.2"))
      (is (= (:at-port drone) 4444))
      (is (= @(:counter drone) 0))))

  ;; (fact "command passes along the data to send-at-command"
  ;;   (let [drone (ar-drone/make-drone)]
  ;;     (ar-drone/connect! drone)
  ;;     (ar-drone/command drone :take-off) => anything
  ;;     (provided
  ;;       (ar-drone/send-at-command drone "AT*REF=2,290718208\r") => 1)))

  ;; (fact "drone-do-for command calls drone command every 30 sec"
  ;;   (let [drone (ar-drone/make-drone)]
  ;;     (ar-drone/drone-do-for drone 1 :take-off) => anything
  ;;     (provided
  ;;       (ar-drone/command drone :take-off nil nil nil nil) => 1 :times #(< 0 %1))))
  )



;; (run-tests 'clj-drone.core-test)
