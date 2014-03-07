(defproject turboshrimp "0.1.0-SNAPSHOT"
  :description "Clojure API for the Parrot AR.Drone."
  :url "https://github.com/wiseman/turboshrimp"
  :license {:name "MIT License"}
  :dependencies [[com.taoensso/timbre "3.1.1"]
                 [de.kotka/lazymap "3.0.0"]
                 [gloss "0.2.2"]
                 [org.clojure/clojure "1.5.1"]]
  :profiles {:dev {:dependencies [[com.lemonodor/xio "0.1.0"]
                                  [criterium "0.4.3"]
                                  [midje "1.6.2"]]
                   :plugins [[lein-midje "3.1.3"]]}
             :test {:jvm-opts ["-server"]}})
