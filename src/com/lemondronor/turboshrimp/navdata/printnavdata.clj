(ns com.lemondronor.turboshrimp.navdata.printnavdata
  (:require [clojure.pprint :as pprint]
            [com.lemondronor.turboshrimp.navdata :as navdata]
            [com.lemonodor.xio :as xio])
  (:gen-class))


(defn -main [& args]
  (let [bytes (xio/binary-slurp System/in)
        navdata (navdata/parse-navdata bytes)]
    (pprint/pprint (into {} navdata))))
