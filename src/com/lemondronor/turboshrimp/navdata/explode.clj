(ns com.lemondronor.turboshrimp.navdata.explode
  (:require [com.lemondronor.turboshrimp.navdata :as navdata]
            [com.lemonodor.xio :as xio])
  (:gen-class))


(defn get-bytes [bb]
  (.array bb))


(defn option-name [type]
  (if (keyword? type)
    (name type)
    (str type)))


(def filename-fmt "navdata-%03d-%s.bin")

(defn -main [& args]
  (let [navdata (xio/binary-slurp (first args))
        [header options] (navdata/navdata-bytes-seq navdata)]
    (xio/binary-spit
     (format filename-fmt 0 "header")
     (get-bytes header))
    (loop [idx 1
           options options]
      (when (seq options)
        (let [[type data] (first options)
              filename (format filename-fmt idx (option-name type))]
          (xio/binary-spit filename (get-bytes data)))
        (recur (inc idx) (rest options))))))
