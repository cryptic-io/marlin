(ns marlin.fs
  (:import java.security.MessageDigest))

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
  the SHA1 hash string of what was read/written against fh"
  [in-stream out-stream fh]
  (let [sha (java.security.MessageDigest/getInstance "SHA-1")
        buf-seq (byte-chunk-seq in-stream)]
    (.reset sha)
    (doseq [buf buf-seq]
      (.write out-stream buf)
      (.update sha buf))
    (let [actual-fh (apply str (map #(format "%02x" %) (.digest sha)))]
      (= fh actual-fh))))
