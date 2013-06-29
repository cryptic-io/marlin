(defproject marlin "0.1.0-SNAPSHOT"
  :description "A simple REST api for file access"
  :url "http://github.com/cryptic-io/marlin"
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [com.taoensso/carmine "2.0.0-beta2"]
                 [compojure "1.1.5"]]
  :plugins [[lein-ring "0.8.5"]]
  :ring {:handler marlin.handler/app}
  :profiles
  {:dev {:dependencies [[ring-mock "0.1.5"]]}})
