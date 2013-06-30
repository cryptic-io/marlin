(ns marlin.core
  (:gen-class)
  (:use [ring.adapter.jetty :only [run-jetty]]
        [clojure.tools.cli :only [cli]])
  (:require [marlin.handler :as handler]
            [marlin.config  :as config]
            [marlin.db      :as db]
            [marlin.fs      :as fs]))

(def description
  "A simple REST api for interacting with a file system, using redis as a backend")

(def default-config-msg
  "Using the default configuration. You can generate this config using the -d flag. You can change the configuration by piping the default one to a file, editing, and passing that file in using the -c flag")

(defn -main [& args]
  (let [[opts _ halp]
          (cli args ["-c" "--config" "Configuration file"]
                    ["-d" "--dump" "Dump default configuration to stdout" :flag true]
                    ["-h" "--help" "Print help" :default false])]

    (cond
      (not (false? (opts :help))) (do (print description "\n\n" halp "\n")
                                      (flush)
                                      (System/exit 0))

      (opts :dump) (do (print config/default-config-str)
                       (flush)
                       (System/exit 0))

      (get opts :config false) (config/load-config (opts :config))
      :else (println default-config-msg))

  (db/init)
  (fs/init)
  (run-jetty handler/app
    (assoc (config/cget :rest)
           :join false))))
