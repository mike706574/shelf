(defproject org.clojars.mike706574/shelf "0.0.1-SNAPSHOT"
  :description "A project."
  :url "https://github.com/mike706574/shelf"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :min-lein-version "2.0.0"
  :dependencies [[com.taoensso/timbre "4.10.0"]
                 [clj-ssh "0.5.14"]]
  :profiles {:dev {:source-paths ["dev"]
                   :target-path "target/dev"
                   :dependencies [[org.clojure/clojure "1.9.0-alpha20"]
                                  [org.clojure/tools.namespace "0.2.11"]]}}
  :repl-options {:init-ns user})
