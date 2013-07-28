(ns marlin.handler
  (:use compojure.core
        [overtone.at-at :only [at now mk-pool]])
  (:require [compojure.handler :as handler]
            [compojure.route :as route]
            [cheshire.core :refer :all]
            [marlin.fs  :as fs]
            [marlin.db  :as db]
            [marlin.log :as log]))

(def at-pool (mk-pool))

(defn- json-200
  [raw]
  { :status 200
    :body (generate-string raw)
    :headers { "Content-Type" "application/json" } })

(defn- text-200
  [raw]
  { :status 200
    :body raw
    :headers { "Content-Type" "text/plain" } })

(defn- json?
  [json]
  (and json (not (= json "0"))))

(defn- interpose-append-str
  [ch l]
  (str (apply str (interpose ch l)) ch))

(defn- set-all-attributes
  [filename size filehash]
  (db/lock-file filename)
  (db/set-file-attributes filename "size" size "hash" filehash ))

(defn sync-db-with-fs
  "Clears the database and loads it with fully correct data about what it's watching
  on the filesystem"
  []
  (db/flushdb)
  (fs/file-walk
    #(let [fullname (fs/full-name %)
           fhash (fs/file-hash fullname)
           fsize (fs/file-size fullname)]
      (set-all-attributes % fsize fhash))))

(defroutes app-routes
  (GET "/" [] "
    Marlin is a REST api which sits on top of a filesystem, making it easy to
    put and delete files and to get information about those files. See
    https://github.com/cryptic-io/marlin for the source/docs\n")

  (PUT "/:fn" {{ filename :fn filehash :hash } :params body :body { content-type "content-type" } :headers}
    (log/info (str "PUT " filename " " filehash " initiated"))
    (if-not (= content-type "application/octet-stream")
      (do (log/warn (str "PUT " filename " not uploaded at octet-stream"))
          {:status 400 :body "PUT file must be of Content-Type 'application/octet-stream'"})

      (if-not (db/lock-file filename)

        ;If we can't lock the file then 400
        (do (log/warn (str "PUT " filename " already exists"))
            {:status 400 :body "File already exists"})

        ;If we can then try to put the file
        (let [path (fs/full-path filename)
              fullname (fs/path-join path filename)]
          (if-not (fs/mkdirs path)
            (do (log/warn (str "PUT " filename " - Could not create directory: " path))
                (db/unlock-file filename)
                {:status 500 :body "Could not create internal directory"})

            (if-let [size (with-open [fileout (java.io.FileOutputStream. fullname)]
                            (fs/safe-read-to-write body fileout filehash))]

                ;If the write was successful we save stuff in db and send back 200
                (do (log/info (str "PUT " filename " successfully"))
                    (set-all-attributes filename size filehash)
                    {:status 200})

                ;If it wasn't we delete what we just wrote and send back 400
                (do (log/warn (str "PUT " filename " failed, incorrect hash"))
                    (.delete (java.io.File. fullname))
                    (db/unlock-file filename)
                    {:status 400 :body "File hash doesn't match"})))))))

  (GET "/all" {{ json :json } :params}
    (let [all (db/get-all-files)]
      (if (json? json)
        (json-200 all)
        (text-200 (interpose-append-str \newline all)))))

  (GET "/sync" {}
    (future (sync-db-with-fs))
    {:status 200})

  (GET "/:fn" {{ filename :fn } :params}
    (let [fullname (fs/full-name filename)]
      (when (.exists (java.io.File. fullname))
        { :status 200
          :body (slurp fullname)
          :headers { "Content-Type" "application/octet-stream" }})))

  (GET "/:fn/all" {{ filename :fn json :json } :params}
    (when-let [all (db/get-all-file-attributes filename)]
      (if (json? json)
        (json-200 all)
        (text-200 (interpose-append-str \newline
                    (map (fn [[k v]] (str k " " v)) all))))))

  (GET "/:fn/:attr" {{ filename :fn attr :attr} :params}
    (when-let [value (db/get-file-attribute filename attr)]
      (text-200 value)))

  (DELETE "/:fn" {{ filename :fn delay-amnt :delay } :params}
    (let [dodel (fn [] (log/info (str "DELETE " filename))
                       (.delete (java.io.File. (fs/full-name filename)))
                       (db/del-file filename)
                       (db/unlock-file filename)
                       {:status 200}) ]
      (if-not (nil? delay-amnt)
        (let [delay-int (Integer/valueOf delay-amnt)]
          (do (log/info (str "DELETE " filename " in " delay-int " milliseconds"))
              (at (+ (now) delay-int) dodel at-pool)
              {:status 200}))
        (dodel))))

  (route/resources "/")
  (route/not-found ""))

(def app
  (handler/api app-routes))
