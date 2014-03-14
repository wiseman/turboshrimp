(ns com.lemondronor.turboshrimp-test
  (:import (java.net DatagramPacket InetAddress))
  (:require [clojure.test :refer :all]
            [midje.sweet :refer :all]
            [com.lemondronor.turboshrimp :as ar-drone]))


(deftest core-tests
  (fact "default initialize gets default host and port"
    (let [drone (ar-drone/make-drone)]
      (.getHostName (:host drone)) => ar-drone/default-hostname
      (:at-port drone) => ar-drone/default-at-port
      @(:counter drone) => 0))

  (fact "custom initiliaze uses custom name host and port"
    (let [drone (ar-drone/make-drone
                 :name :frank
                 :hostname "192.168.2.2"
                 :at-port 4444)]
      (.getHostName (:host drone)) => "192.168.2.2"
      (:at-port drone) => 4444
      @(:counter drone) => 0))

  (fact "command passes along the data to send-at-command"
    (let [drone (ar-drone/make-drone)]
      (ar-drone/connect! drone)
      (ar-drone/command drone :take-off) => anything
      (provided
        (ar-drone/send-at-command drone "AT*REF=2,290718208\r") => 1)))

  (fact "drone-do-for command calls drone command every 30 sec"
    (let [drone (ar-drone/make-drone)]
      (ar-drone/drone-do-for drone 1 :take-off) => anything
      (provided
        (ar-drone/command drone :take-off nil nil nil nil) => 1 :times #(< 0 %1)))))



;; (run-tests 'clj-drone.core-test)
