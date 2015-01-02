(ns com.lemondronor.turboshrimp.util-test
  (:require [clojure.test :refer :all]
            [com.lemondronor.turboshrimp.util :as util]))


(deftest util-tests
  (testing "Scheduled tasks"
    (let [pool (util/make-sched-thread-pool 1)]
      (let [p (promise)]
        (util/execute-in-pool pool #(deliver p :delivered))
        (is (= :delivered @p)))
      (let [c (atom 0)
            task (util/periodic-task
                  50
                  #(swap! c inc) pool)]
        (Thread/sleep 125)
        (util/cancel-scheduled-task task)
        (is (= 3 @c)))
      (util/shutdown-pool pool))))
