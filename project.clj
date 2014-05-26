(defproject turboshrimp "0.1.0-SNAPSHOT"
  :description "Clojure API for the Parrot AR.Drone."
  :url "https://github.com/wiseman/turboshrimp"
  :license {:name "MIT License"}
  :dependencies [[ch.qos.logback/logback-classic "1.1.1"]
                 [com.lemonodor/xio "0.2.2"]
                 [de.kotka/lazymap "3.1.1"]
                 [gloss "0.2.2"]
                 [org.clojure/clojure "1.6.0"]
                 [org.clojure/tools.logging "0.2.6"]]
  :profiles {:dev {:dependencies [[criterium "0.4.3"]
                                  [org.clojars.echo/test.mock "0.1.2"]]
                   :plugins [[lein-cloverage "1.0.2"]]}
             :test {:jvm-opts ["-server"]}})
