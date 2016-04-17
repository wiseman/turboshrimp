(ns ^:no-doc com.lemondronor.turboshrimp.util
  (:import (java.util.concurrent ScheduledFuture
                                 ScheduledThreadPoolExecutor
                                 ThreadFactory TimeUnit)))

(set! *warn-on-reflection* true)


(defn make-sched-thread-pool [^long num-threads]
  (ScheduledThreadPoolExecutor.
   num-threads
   ^ThreadFactory (proxy [ThreadFactory] []
                    (newThread [^Runnable runnable]
                      (doto (Thread. runnable)
                        (.setDaemon true))))))


(defn execute-in-pool [^ScheduledThreadPoolExecutor pool fn]
  (.execute pool fn))

(defn shutdown-pool [^ScheduledThreadPoolExecutor pool]
  (.shutdown pool))


(defn periodic-task [period fn ^ScheduledThreadPoolExecutor pool]
  (.scheduleAtFixedRate pool fn 0 period TimeUnit/MILLISECONDS))

(defn cancel-scheduled-task [^ScheduledFuture task]
  (.cancel task false))
