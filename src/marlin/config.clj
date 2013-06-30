(ns marlin.config
    (:require [clojure.tools.reader.edn :as edn]))

(def default-config-str "
; This is an example configuration for marlin. It contains all the default values for the various
; options, change them as needed.
{

    ;Information marlin needs to talk to its redis server
    :redis {
             ;Hostname redis lives on. Could also be ip-address (also as string)
             :host \"localhost\"

             ;Port redis lives on
             :port 6379

             ;Database number that you want to use
             :db 0

           }

    ;Options for the HTTP REST API (OMG CAPS)
    :rest {
            :host \"0.0.0.0\"
            :port 3000
          }

    ;The root of the structure marlin will put files in
    :root \"/tmp/marlin\"

}
")

(def config-atom (atom {}))

(defn cget
    "Given key you want from the config, returns that key. Key can be multiple layers deep, for
    instance: (cget :redis :host)"
    [& keys]
    (get-in @config-atom keys))

(defn put-default-config
    "Given a filename puts the example config there (complete with comments)"
    [filename]
    (spit filename default-config-str))

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

(load-config-str default-config-str)
