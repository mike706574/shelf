(defproject fun.mike/shelf "0.0.8-SNAPSHOT"
  :description "A project."
  :url "https://github.com/mike706574/shelf"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/spec.alpha "0.2.194"]
                 [com.taoensso/timbre "5.1.2"]
                 [clj-ssh "0.5.14"]
                 [commons-codec "1.15"]]
  :profiles {:dev {:source-paths ["dev"]
                   :target-path "target/dev"
                   :dependencies [[org.clojure/clojure "1.10.2"]
                                  [org.clojure/tools.namespace "1.1.0"]]}}
  :repositories [["releases" {:url "https://clojars.org/repo"
                              :creds :gpg}]]
  :repl-options {:init-ns user})
