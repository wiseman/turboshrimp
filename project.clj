(defproject com.lemondronor/turboshrimp "0.3.5"
  :description "Clojure API for the Parrot AR.Drone."
  :url "https://github.com/wiseman/turboshrimp"
  :license {:name "MIT License"}
  :deploy-repositories [["releases" :clojars]]
  :dependencies [[com.lemonodor/xio "0.2.2"]
                 [de.kotka/lazymap "3.1.1"]
                 [gloss "0.2.4"]
                 [org.clojure/clojure "1.6.0"]
                 [org.clojure/tools.logging "0.3.1"]]
  :java-source-paths ["src/java"]
  :profiles {:dev {:dependencies [[criterium "0.4.3"]
                                  [org.clojars.echo/test.mock "0.1.2"]]
                   :plugins [[lein-cloverage "1.0.2"]]}
             :test {:jvm-opts ["-server"]}
             :uberjar {:aot :all}})
