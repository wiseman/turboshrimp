(defproject turboshrimp "0.1.0-SNAPSHOT"
  :description "Clojure API for the Parrot AR.Drone."
  :url "https://github.com/wiseman/turboshrimp"
  :license {:name "MIT License"}
  :dependencies [[com.taoensso/timbre "3.1.1"]
                 [org.clojure/clojure "1.5.1"]]
  :profiles {:dev {:dependencies [[com.lemonodor/xio "0.1.0"]
                                  [midje "1.6.2"]]
                   :plugins [[lein-midje "3.1.3"]]}})
