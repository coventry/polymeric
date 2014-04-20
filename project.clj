(defproject graft "0.1.0-SNAPSHOT"
  :description "FIXME: write this!"
  :url "http://example.com/FIXME"

  :profiles {:dev {:plugins [[com.cemerick/austin "0.1.4"]]}}
  
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/clojurescript "0.0-2156"]
                 [com.cemerick/clojurescript.test "0.3.0"]
                 [org.clojure/core.async "0.1.267.0-0d7780-alpha"]
                 [com.cemerick/double-check "0.5.7-SNAPSHOT"]
                 [om "0.5.0"]]

  :plugins [[lein-cljsbuild "1.0.2"]]

  :source-paths ["src" "test"]

  :cljsbuild { 
    :builds [{:id "graft"
              :source-paths ["src" "test"]
              :compiler {
                :libs [""]
                :output-to "graft.js"
                :output-dir "out"
                :optimizations :none
                :source-map true}}]})
