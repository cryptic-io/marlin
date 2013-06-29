(ns marlin.handler
  (:use compojure.core)
  (:require [compojure.handler :as handler]
            [compojure.route :as route]))

(defn lazy-test
  ([] (lazy-test 1))
  ([n] (when (< n 5) (cons n (lazy-seq (lazy-test (inc n)))))))
(take 8 (lazy-test))

(defn- read-or-nil [stream]
  (let [buf (byte-array 2)]
    (when-not (= -1 (.read stream buf))
      buf)))

(defn byte-chunk-seq [stream]
  (cons (read-or-nil stream) (lazy-seq (byte-chunk-seq stream))))

(with-open [rdr (java.io.FileInputStream. "/tmp/wut")]
  (take 2 (byte-chunk-seq rdr)))

(defn safe-read-to-file
  [stream fh]
  (str (byte-chunk-seq stream)))
  ;;(let [sha java.security.MessageDigest/getInstance "SHA-1"
  ;;      buf (byte-array 1024)]
  ;;  (.reset sha)
  ;;  (doall
  ;;    (reduce (fn [sha 

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
