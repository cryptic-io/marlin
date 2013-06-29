(ns marlin.handler
  (:use compojure.core)
  (:require [compojure.handler :as handler]
            [compojure.route :as route]
            [marlin.fs :as fs]
            [marlin.db :as db]))

(comment
(with-open [rdr (java.io.FileInputStream. "/tmp/wut")
            wtr (java.io.FileOutputStream. "/tmp/wut2")]
  (fs/safe-read-to-write rdr wtr "aa18a208c7f07eb809473b27beae91777c1c7fbf"))
)

(defroutes app-routes
  (GET "/" [] "Some info would probably go here")

  (PUT "/:fn" {{ filename :fn filehash :hash } :params body :body}
    (if-not (db/lock-file filename)

      {:status 400 :body "File already exists"}
      (let [path (fs/full-path filename)
            fullname (fs/path-join path filename)]
        (.mkdirs (java.io.File. path))
        (if-let [size (with-open [fileout (java.io.FileOutputStream. fullname)]
                        (fs/safe-read-to-write body fileout filehash))]

            ;If the write was successful we save stuff in db and send back 200
            (do (db/set-file-size filename size)
                (db/set-file-hash filename filehash)
                {:status 200})

            ;If it wasn't we delete what we just wrote and send back 400
            (do (.delete (java.io.File. fullname))
                {:status 400 :body "File hash doesn't match"})))))

  (route/resources "/")
  (route/not-found "Not Found"))

(def app
  (handler/site app-routes))
