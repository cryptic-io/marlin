(ns marlin.db
  (:require [taoensso.carmine :as car :refer (wcar)]
            [clojure.string :as s]))

(def server1-conn {:pool {} :spec {:host "localhost" :port 6379}})
(defmacro wcar* [& body] `(car/wcar server1-conn ~@body))

(defn- file-metadata-key [filename] (str "metadata:" filename))
(defn- file-lock-key [filename] (str "lock:" filename))

(defn lock-file
  "Attempts to lock a file. Returns true if lock successful, false if already locked"
  [filename]
  (let [k (file-lock-key filename)]
    (not (zero? (wcar* (car/setnx k 1))))))

(defn unlock-file
  "Unlocks a file if it was locked. Not sure why this would ever be necessary."
  [filename]
  (let [k (file-lock-key filename)]
    (wcar* (car/hdel k))
    nil))

(defn set-file-attributes
  "Sets an attribute for a file"
  [filename & keyvals]
  (let [k (file-metadata-key filename)]
    (wcar* (apply car/hmset k keyvals))
    nil))

(defn get-file-attribute
  "Gets an attribute for a file (nil if attribute not set or file doesn't exist)"
  [filename attr]
  (let [k (file-metadata-key filename)]
    (wcar* (car/hget k attr))))

(defn get-all-file-attributes
  "Gets all attributes for a file as a map, or nil if file doesn't exist"
  [filename]
  (let [k (file-metadata-key filename)
        r (reduce (fn [m [k v]] (assoc m k v)) {}
            (partition 2 (wcar* (car/hgetall k))))]
    (when-not (empty? r) r)))

(defn get-all-files
  "Returns list of all files in database"
  []
  (map #(second (s/split % #":"))
    (wcar* (car/keys (file-metadata-key "*")))))
