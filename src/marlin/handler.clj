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
    (safe-read-to-file body filehash))

  (route/resources "/")
  (route/not-found "Not Found"))

(def app
  (handler/site app-routes))

(import 'java.security.MessageDigest)

(defn sha1-test [s]
     (doto (java.security.MessageDigest/getInstance "SHA-1")
                    ;.reset
                    (.update (.getBytes s))
                    (.digest)))

(sha1-test "wut")
