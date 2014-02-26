(ns com.lemondronor.turboshrimp-test
  (:import (java.net DatagramPacket InetAddress))
  (:require [clojure.test :refer :all]
            [midje.sweet :refer :all]
            [com.lemondronor.turboshrimp :as ar-drone]))


(deftest core-tests
  (fact "default initialize gets default host and port"
    (.getHostName (:host (:default @ar-drone/drones))) => ar-drone/default-drone-ip
    (:at-port (:default @ar-drone/drones)) => ar-drone/default-at-port
    @(:counter (:default @ar-drone/drones)) => 1
    (against-background (before :facts (ar-drone/drone-initialize))))

  (fact "custom initiliaze uses custom name host and port"
    (.getHostName (:host (:frank @ar-drone/drones))) => "192.168.2.2"
    (:at-port (:frank @ar-drone/drones)) => 4444
    @(:counter (:frank @ar-drone/drones)) => 1
    (against-background (before :facts (ar-drone/drone-initialize :frank "192.168.2.2" 4444))))

  (fact "drone command passes along the data to send-command"
    (ar-drone/drone :take-off) => anything
    (provided
      (ar-drone/send-command :default "AT*REF=2,290718208\r") => 1)
    (against-background (before :facts (ar-drone/drone-initialize))))

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
