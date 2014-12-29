(defproject com.lemondronor/turboshrimp "0.3.1-SNAPSHOT"
  :description "Clojure API for the Parrot AR.Drone."
  :url "https://github.com/wiseman/turboshrimp"
  :license {:name "MIT License"}
  :repositories [["xuggle" {:url "http://xuggle.googlecode.com/svn/trunk/repo/share/java/"
                            :checksum :ignore}]]
  :dependencies [[com.lemondronor/turboshrimp-h264j "0.1.0-SNAPSHOT16"]
                 [com.lemonodor/xio "0.2.2"]
                 [com.twilight/h264 "0.0.1"]
                 [de.kotka/lazymap "3.1.1"]
                 [gloss "0.2.4"]
                 [org.clojure/clojure "1.6.0"]
                 [org.clojure/tools.logging "0.3.1"]
                 [xuggle/xuggle-xuggler "5.2"]]
  :java-source-paths ["src/java"]
  :profiles {:dev {:dependencies [[criterium "0.4.3"]
                                  [org.clojars.echo/test.mock "0.1.2"]]
                   :plugins [[lein-cloverage "1.0.2"]]}
             :test {:jvm-opts ["-server"]}
             :uberjar {:aot :all}})
