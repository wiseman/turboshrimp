(defproject com.lemondronor/turboshrimp "2.1.1"
  :description "Clojure API for the Parrot AR.Drone 2.0.

  By John Wiseman / jjwiseman@gmail.com / http://twitter.com/lemonodor"
  :url "https://github.com/wiseman/turboshrimp"
  :license {:name "MIT License"
            :url "http://opensource.org/licenses/MIT"}
  :deploy-repositories [["releases" :clojars]]
  :dependencies [[de.kotka/lazymap "3.1.1"]
                 [gloss "0.2.6"]
                 [org.clojure/clojure "1.8.0"]
                 [org.clojure/tools.logging "0.3.1"]]
  :java-source-paths ["src/java"]
  :profiles {:dev {:dependencies [[com.lemonodor/xio "0.2.2"]
                                  [org.clojars.echo/test.mock "0.1.2"]]
                   :plugins [[lein-cloverage "1.0.2"]
                             [lein-codox "0.9.4"]]
                   :codox {:metadata {:doc/format :markdown}
                           :project {:version "2.0.3"}
                           :html {:namespace-list :flat}
                           :source-uri "https://github.com/wiseman/turboshrimp/blob/{version}/{filepath}#L{line}"}}
             :example {:dependencies
                       [[com.lemondronor/turboshrimp-xuggler "0.0.4"]
                        [com.lemonodor/gflags "0.7.3"]
                        [com.lemonodor/xio "0.2.2"]
                        [seesaw "1.4.4"]]
                       :source-paths ["examples"]}
             :uberjar {:aot :all}})
