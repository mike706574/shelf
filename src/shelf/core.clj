(ns shelf.core
  (:require [clj-ssh.ssh :as ssh]
            [clojure.java.shell :as sh]
            [clojure.spec.alpha :as s]
            [clojure.string :as str]
            [taoensso.timbre :as log]))

(defprotocol Shell
  (execute [this args]))

(defn ^:private log-response
  [{:keys [exit out err] :as response}]
  (log/debug (str "Exit Code: " exit))
  (log/trace (str "Out:\n" out))
  (log/trace (str "Error:\n" err))
  response)

(def ^:private output-re #"(?s).*___START_OUT___\n(.*)___END_OUT___.*")

(defn ^:private output [s] (second (re-matches output-re s)))
(defn ^:private unix-eol [s] (.replace s "\r\n" "\n"))

(defrecord SecureShell [session]
  Shell
  (execute [this args]
    (let [command (str/join " " args)]
      (log/debug (str "Executing command: " command))
      (let [wrapped-command (str "echo \"___START_OUT___\"; " command "; echo \"___END_OUT___\"")
            {exit :exit :as response} (ssh/ssh session {:in wrapped-command})]
        (log-response response)
        (-> response
            (update :out (comp output unix-eol))
            (merge (if (zero? exit)
                     {:status :ok :ok true}
                     {:status :failure :ok false})))))))

(defrecord LocalShell []
  Shell
  (execute [this args]
    (log/debug (str "Executing command: " (str/join " " args)))
    (let [{exit :exit :as response} (apply sh/sh args)]
      (log-response response)
      (merge response (if (zero? exit)
                        {:status :ok :ok true}
                        {:status :failure :ok false})))))

(defn local [] (LocalShell.))

(defn ssh
  [session]
  (SecureShell. session))

(defn session [{:keys [:shelf/host :shelf/username :shelf/password]}]
  (ssh/session (ssh/ssh-agent {}) host {:username username
                                        :password password
                                        :strict-host-key-checking :no}))

(defn config [host username password]
  {:shelf/host host
   :shelf/username username
   :shelf/password password})

(defn exec [shell args] (execute shell args))

(defmacro with-ssh
  [config & body]
  `(let [session# (session ~config)]
     (try
       (let [~'shell (ssh session#)]
           (when-not (ssh/connected? session#)
             (ssh/connect session#))
           ~@body)
       (finally
        (ssh/disconnect session#)))))

(s/def :shelf/host string?)
(s/def :shelf/username string?)
(s/def :shelf/password string?)
(s/def :shelf/config (s/keys :req [:shelf/host :shelf/username :shelf/password]))
