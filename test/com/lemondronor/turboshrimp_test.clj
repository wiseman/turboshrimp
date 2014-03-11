(ns com.lemondronor.turboshrimp-test
  (:import (java.net DatagramPacket InetAddress))
  (:require [clojure.test :refer :all]
            [midje.sweet :refer :all]
            [com.lemondronor.turboshrimp :as ar-drone]))


(deftest core-tests
  (fact "default initialize gets default host and port"
    (let [drone (ar-drone/drone-initialize)]
      (.getHostName (:host drone)) => ar-drone/default-hostname
      (:at-port drone) => ar-drone/default-at-port
      @(:counter drone) => 0))

  (fact "custom initiliaze uses custom name host and port"
    (let [drone (ar-drone/drone-initialize
                 :name :frank
                 :hostname "192.168.2.2"
                 :at-port 4444)]
      (.getHostName (:host drone)) => "192.168.2.2"
      (:at-port drone) => 4444
      @(:counter drone) => 0))

  (fact "drone command passes along the data to send-command"
    (let [drone (ar-drone/connect (ar-drone/drone-initialize))]
      (ar-drone/drone :take-off) => anything
      (provided
        (ar-drone/send-command :default "AT*REF=2,290718208\r") => 1)))

  (fact "drone-do-for command calls drone command every 30 sec"
    (ar-drone/drone-do-for 1 :take-off) => anything
    (provided
      (ar-drone/mdrone :default :take-off nil nil nil nil) => 1 :times #(< 0 %1))
    (against-background (before :facts (ar-drone/drone-initialize))))

  (fact "find-drone finds the drone by ip"
    (ar-drone/find-drone "192.168.1.2") => {:drone2 {:host (InetAddress/getByName"192.168.1.2")}}
    (against-background
      (before :facts
              (reset! ar-drone/drones
                      {:drone1 {:host
                                (InetAddress/getByName "192.168.1.1")}
                       :drone2 {:host
                                (InetAddress/getByName"192.168.1.2")}})))))



;; (run-tests 'clj-drone.core-test)
