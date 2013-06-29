(ns marlin.db
  (:require [taoensso.carmine :as car :refer (wcar)]))

(def server1-conn {:pool {} :spec {:host "localhost" :port 6379}})
(defmacro wcar* [& body] `(car/wcar server1-conn ~@body))

(defn- file-key [file-name] (str "file:" file-name))

(defn lock-file
  "Attempts to lock a file. Returns true if lock successful, false if already locked"
  [filename]
  (let [k (file-key filename)]
    (not (zero? (wcar* (car/hsetnx k "locked" 1))))))

(defn unlock-file
  "Unlocks a file if it was locked. Not sure why this would ever be necessary."
  [filename]
  (let [k (file-key filename)]
    (wcar* (car/hdel k "locked"))
    nil))

(defn set-file-size
  "Sets the size of the file in bytes"
  [filename size]
  (let [k (file-key filename)]
    (wcar* (car/hset k "size" size))
    nil))

(defn get-file-size
  "Gets the size of the file in bytes, or nil"
  [filename]
  (let [k (file-key filename)]
    (when-let [sizestr (wcar* (car/hget k "size"))]
      (Integer/valueOf sizestr))))

(defn set-file-hash
  "Sets the hash string of the file"
  [filename fh]
  (let [k (file-key filename)]
    (wcar* (car/hset k "hash" fh))
    nil))

(defn get-file-hash
  "Gets the hash string of the file, or nil"
  [filename]
  (let [k (file-key filename)]
    (wcar* (car/hget k "hash"))))
