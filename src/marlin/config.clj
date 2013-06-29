(ns marlin.config
    (:require [clojure.tools.reader.edn :as edn]))

(def example-config-str "
; This is an example configuration for marlin. It contains all the default values for the various
; options, change them as needed.
{

    ;Information marlin needs to talk to its redis server
    :redis {
             ;Hostname redis lives on. Could also be ip-address (also as string)
             :host \"localhost\"

             ;Port redis lives on
             :port 3306

             ;Database number that you want to use
             :db 0

           }

    ;Options for the HTTP REST API (OMG CAPS)
    :rest {
            :port 3000
          }

}
")

(def config-atom (atom {}))

(defn get
    "Given key you want from the config, returns that key. Key can be multiple layers deep, for
    instance: (get :redis :host)"
    [& keys]
    (get-in @config-atom keys))

(defn put-default-config
    "Given a filename puts the example config there (complete with comments)"
    [filename]
    (spit filename example-config-str))

(defn load-config-str
    "Given a string, loads it in as configuration"
    [config-str]
    (->> config-str
         (edn/read-string)
         (reset! config-atom)))

(defn load-config
    "Given a filename, loads it in as configuration"
    [filename]
    (-> filename
        (slurp)
        (load-config-str)))

(load-config-str example-config-str)
