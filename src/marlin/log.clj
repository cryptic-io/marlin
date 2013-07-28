(ns marlin.log)
  ;(:import org.eclipse.jetty.util.log.Log))

(def debug (partial println "DEBUG"))
(def info  (partial println "INFO"))
(def warn  (partial println "WARN"))
