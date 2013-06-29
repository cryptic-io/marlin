(ns marlin.handler
  (:use compojure.core)
  (:require [compojure.handler :as handler]
            [compojure.route :as route]
            [marlin.fs :as fs]))

(comment
(with-open [rdr (java.io.FileInputStream. "/tmp/wut")
            wtr (java.io.FileOutputStream. "/tmp/wut2")]
  (fs/safe-read-to-write rdr wtr "aa18a208c7f07eb809473b27beae91777c1c7fbf"))
)

(defroutes app-routes
  (GET "/" [] "Some info would probably go here")

  (PUT "/:fn" {{ filename :fn filehash :hash } :params body :body}
    (let [path (fs/full-path filename)
          fullname (fs/path-join path filename)]
      (.mkdirs (java.io.File. path))
      (if (with-open [fileout (java.io.FileOutputStream. fullname)]
            (fs/safe-read-to-write body fileout filehash))

          ;If the write was successful we send back 200
          {:status 200}

          ;If it wasn't we delete what we just wrote and send back 400
          (do (.delete (java.io.File. fullname))
              {:status 400}))))

  (route/resources "/")
  (route/not-found "Not Found"))

(def app
  (handler/site app-routes))
