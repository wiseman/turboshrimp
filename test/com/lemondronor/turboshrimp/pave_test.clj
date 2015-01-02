(ns com.lemondronor.turboshrimp.pave-test
  (:require [clojure.java.io :as io]
            [clojure.test :refer :all]
            [com.lemondronor.turboshrimp.pave :as pave]))


(deftest pave-tests
  (testing "i-frame?"
    (is (pave/i-frame? {:frame-type pave/I-FRAME :slice-index 0}))
    (is (pave/i-frame? {:frame-type pave/I-FRAME :slice-index 10}))
    (is (pave/i-frame? {:frame-type pave/IDR-FRAME :slice-index 0}))
    (is (not (pave/i-frame? {:frame-type pave/IDR-FRAME :slice-index 10})))
    (is (not (pave/i-frame? {:frame-type pave/P-FRAME :slice-index 0})))
    (is (not (pave/i-frame? {:frame-type pave/P-FRAME :slice-index 10}))))
  (testing "read-frame"
    (let [frame (-> "1-frame.pave"
                    io/resource
                    io/input-stream
                    pave/read-frame)]
      (is (= 76 (:header-size frame)))
      (is (= [640 360] (:display-dimensions frame)))
      (is (= [640 368] (:encoded-dimensions frame)))
      (is (= 6715 (:frame-number frame)))
      (is (= 224820 (:timestamp frame)))
      (is (= pave/IDR-FRAME (:frame-type frame)))
      (is (= 0 (:slice-index frame)))
      (is (instance? (type (byte-array [])) (:payload frame)))
      (is (= 13556 (count (:payload frame))))))
  (testing "frame queue"
    (let [fq (pave/make-frame-queue)]
      (is (= nil (pave/pull-frame fq 1)))
      (let [f1 {:frame-type pave/P-FRAME :payload 1}
            f2 {:frame-type pave/P-FRAME :payload 2}
            f3 {:frame-type pave/I-FRAME :payload 3}]
        (pave/queue-frame fq f1)
        (is (= f1 (pave/pull-frame fq)))
        (is (= nil (pave/pull-frame fq 1)))
        (pave/queue-frame fq f1)
        (pave/queue-frame fq f2)
        (is (= f1 (pave/pull-frame fq)))
        (is (= f2 (pave/pull-frame fq)))
        (is (= nil (pave/pull-frame fq 1)))
        (pave/queue-frame fq f1)
        (pave/queue-frame fq f2)
        (pave/queue-frame fq f3)
        (is (= f3 (pave/pull-frame fq)))
        (is (= nil (pave/pull-frame fq 1)))))
    (let [fq (pave/make-frame-queue :reduce-latency? false)]
      (is (= nil (pave/pull-frame fq 1)))
      (let [f1 {:frame-type pave/P-FRAME :payload 1}
            f2 {:frame-type pave/P-FRAME :payload 2}
            f3 {:frame-type pave/I-FRAME :payload 3}]
        (pave/queue-frame fq f1)
        (is (= f1 (pave/pull-frame fq)))
        (is (= nil (pave/pull-frame fq 1)))
        (pave/queue-frame fq f1)
        (pave/queue-frame fq f2)
        (is (= f1 (pave/pull-frame fq)))
        (is (= f2 (pave/pull-frame fq)))
        (is (= nil (pave/pull-frame fq 1)))
        (pave/queue-frame fq f1)
        (pave/queue-frame fq f2)
        (pave/queue-frame fq f3)
        (is (= f1 (pave/pull-frame fq)))
        (is (= f2 (pave/pull-frame fq)))
        (is (= f3 (pave/pull-frame fq)))
        (is (= nil (pave/pull-frame fq 1)))))))
