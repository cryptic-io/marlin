(ns marlin.handler
  (:use compojure.core)
  (:require [compojure.handler :as handler]
            [compojure.route :as route]
            [cheshire.core :refer :all]
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

      ;If we can't lock the file then 400
      {:status 400 :body "File already exists"}

      ;If we can then try to put the file
      (let [path (fs/full-path filename)
            fullname (fs/path-join path filename)]
        (.mkdirs (java.io.File. path))
        (if-let [size (with-open [fileout (java.io.FileOutputStream. fullname)]
                        (fs/safe-read-to-write body fileout filehash))]

            ;If the write was successful we save stuff in db and send back 200
            (do (db/set-file-attribute filename "size" size)
                (db/set-file-attribute filename "hash" filehash)
                {:status 200})

            ;If it wasn't we delete what we just wrote and send back 400
            (do (.delete (java.io.File. fullname))
                {:status 400 :body "File hash doesn't match"})))))

  (GET "/all" {{ json :json } :params}
    (let [all (db/get-all-files)]
      (if (and json (not (= json "0")))
        {:status 200 :body (generate-string all)}
        {:status 200 :body (apply str (interpose \newline all))})))

  (GET "/:fn" {{ filename :fn } :params}
    (let [fullname (fs/full-name filename)]
      (if (.exists (java.io.File. fullname))
        {:status 200 :body (slurp fullname)}
        {:status 404})))

  (GET "/:fn/all" {{ filename :fn } :params}
    (if-let [all (db/get-all-file-attributes filename)]
      {:status 200 :body (generate-string all)}
      {:status 404}))

  (GET "/:fn/:attr" {{ filename :fn attr :attr} :params}
    (if-let [value (db/get-file-attribute filename attr)]
      {:status 200 :body value}
      {:status 404}))

  (route/resources "/")
  (route/not-found "Not Found"))

(def app
  (handler/site app-routes))
