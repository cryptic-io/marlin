(ns marlin.db
  (:require [taoensso.carmine :as car :refer (wcar)]
            [clojure.string :as s]
            [marlin.config :as config]))

(defn opts []
  {:pool {} :spec (config/cget :redis)})

(def conn (atom nil))
(defn init []
  (reset! conn (opts)))
(init)

(defmacro wcar* [& body] `(car/wcar @conn ~@body))

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
    (wcar* (car/del k))
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

(defn del-file
  "Deletes a file's metadata entry"
  [filename]
  (let [k (file-metadata-key filename)]
    (wcar* (car/del k))
    nil))

(defn flushdb
  "Deletes everything in the db"
  [] (wcar* (car/flushall)))
