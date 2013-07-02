(ns marlin.handler
  (:use compojure.core
        [overtone.at-at :only [at now mk-pool]])
  (:require [compojure.handler :as handler]
            [compojure.route :as route]
            [cheshire.core :refer :all]
            [marlin.fs :as fs]
            [marlin.db :as db]))

(def at-pool (mk-pool))

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
            (do (db/set-file-attributes filename "size" size "hash" filehash)
                {:status 200})

            ;If it wasn't we delete what we just wrote and send back 400
            (do (.delete (java.io.File. fullname))
                (db/unlock-file filename)
                {:status 400 :body "File hash doesn't match"})))))

  (GET "/all" {{ json :json } :params}
    (let [all (db/get-all-files)]
      (if (and json (not (= json "0")))
        {:status 200 :body (generate-string all)}
        {:status 200 :body (apply str (interpose \newline all))})))

  (GET "/allattributes" {}
    (generate-string
      (reduce #(assoc %1 %2 (db/get-all-file-attributes %2)) {} (db/get-all-files))))

  (GET "/:fn" {{ filename :fn } :params}
    (let [fullname (fs/full-name filename)]
      (when (.exists (java.io.File. fullname))
        {:status 200 :body (slurp fullname)})))

  (GET "/:fn/all" {{ filename :fn } :params}
    (when-let [all (db/get-all-file-attributes filename)]
      {:status 200 :body (generate-string all)}))

  (GET "/:fn/:attr" {{ filename :fn attr :attr} :params}
    (when-let [value (db/get-file-attribute filename attr)]
      {:status 200 :body value}))

  (DELETE "/:fn" {{ filename :fn delay-amnt :delay } :params}
    (let [dodel (fn []
      (.delete (java.io.File. (fs/full-name filename)))
      (db/del-file filename)
      (db/unlock-file filename)
      {:status 200}
      )]
      (if-not (nil? delay-amnt)
        (do (at (+ (now) (Integer/valueOf delay-amnt)) dodel at-pool) {:status 200})
        (dodel))))

  (route/resources "/")
  (route/not-found ""))

(def app
  (handler/api app-routes))
