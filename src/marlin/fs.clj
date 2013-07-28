(ns marlin.fs
  (:import java.security.MessageDigest)
  (:require [marlin.config :as config]))

(def BUFSIZE 1024)
(defn- read-or-nil
  "Reads out a byte-array of BUFSIZE or less from the given InputStream, or nil
  if there's no data left"
  [stream]
  (let [buf (byte-array BUFSIZE)
        cnt (.read stream buf)]
    (if (= BUFSIZE cnt)
      buf                 ; - We filled the buffer, we can just return it
      (cond (> 0 cnt) nil ; - Less than zero means the stream is closed
            (= 0 cnt) '() ; - Zero just means nothing currently on the stream
            :else         ; - Otherwise copy what we got into a buffer of right size
              (java.util.Arrays/copyOfRange buf 0 cnt)))))

(defn- byte-chunk-seq
  "Given an InputStream returns a lazy sequence of byte-arrays from reading it"
  [stream]
  (lazy-seq (when-let [buf (read-or-nil stream)]
    (cons buf (byte-chunk-seq stream)))))

(defn safe-read-to-write
  "Reads in-stream (an InputStream) into out-stream (an OutputStream), and checks
  the SHA1 hash string of what was read/written against fh. Returns the amount
  transferred if the hash checks out, false otherwise"
  [in-stream out-stream fh]
  (let [
    sha (java.security.MessageDigest/getInstance "SHA-1")
    buf-seq (byte-chunk-seq in-stream)
    size
      (reduce
        (fn [sizeacc buf]
          (.write out-stream buf)
          (.update sha buf)
          (+ sizeacc (count buf)))
        0 buf-seq)
    actual-fh
      (apply str (map #(format "%02x" %) (.digest sha)))]

    (and (= fh actual-fh) size)))

(defn path-join
  "Joins all variatic arguments into a properly formatted filesystem path"
  [parent & children]
  (.getPath
    (reduce
      (fn [f filename] (java.io.File. f filename))
      (java.io.File. parent)
      children)))

(def root (atom nil))
(defn init []
  (reset! root (config/cget :root)))
(init)

(defn full-path
  "Returns the full path that will house a file"
  [filename]
  (let [first-letters (map str (take 3 filename))]
    (apply path-join @root first-letters)))

(defn full-name
  "Returns the absolute path of where a file will be housed"
  [filename]
  (path-join (full-path filename) filename))

(defn file-hash
  "Returns the sha1 hash string of the given file path"
  [filepath]
  (let [sha (java.security.MessageDigest/getInstance "SHA-1")]
    (with-open [rdr (java.io.FileInputStream. filepath)]
      (doseq [buf (byte-chunk-seq rdr)] (.update sha buf))
      (apply str (map #(format "%02x" %) (.digest sha))))))

(defn file-size
  "Returns the size in bytes of the given file path"
  [filepath]
  (.length (java.io.File. filepath)))

(defn file-walk
  "Goes through all the files in marlin's filesystem and runs (fun <filestr>) on each
  one, where <filestr> is the absolute filename as a string"
  [fun]
  (let [filenames (->> (java.io.File. @root)
                       (file-seq)
                       (remove #(.isDirectory %))
                       (map #(.getName %))
                       )]
    (doseq [filename filenames] (fun filename))))

(defn mkdirs
  "Does the equivalent of mkdir -p on linux. Returns true if the directory was successfully created
  or already existed, false otherwise"
  [dir]
  (let [dirf (java.io.File. dir)]
    (or (.exists dirf) (.mkdirs dirf))))
