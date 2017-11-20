(ns shelf.core
  (:require [clj-ssh.ssh :as ssh]
            [clojure.java.shell :as sh]
            [clojure.spec.alpha :as s]
            [clojure.string :as str]
            [shelf.crypto :as crypto]
            [taoensso.timbre :as log]))

(s/def :shelf/host string?)
(s/def :shelf/username string?)
(s/def :shelf/password string?)

(s/def :shelf/basic-ssh-config (s/keys :req [:shelf/host :shelf/username :shelf/password]))

(defmulti shelf-type :shelf/type)
(defmethod shelf-type "local" [_] (s/keys))
(defmethod shelf-type "multi-session-basic-ssh" [_] :shelf/basic-ssh-config)
(defmethod shelf-type "single-session-basic-ssh" [_] :shelf/basic-ssh-config)

(s/def :shelf/config (s/multi-spec shelf-type :shelf/type))

(defprotocol Shell
  (execute [this args]))

(defn shell? [x] (instance? shelf.core.Shell x))
(s/def :shelf/shell shell?)

(defn ^:private log-response
  [{:keys [exit out err] :as response}]
  (log/debug (str "Exit Code: " exit))
  (log/trace (str "Out:\n" out))
  (log/trace (str "Error:\n" err))
  response)

(def ^:private output-re #"(?s).*___START_OUT___\n(.*)___END_OUT___.*")

(defn ^:private output [s] (second (re-matches output-re s)))
(defn ^:private unix-eol [s] (.replace s "\r\n" "\n"))

(defn ^:private execute-in-session
  "Executes the command composed of args in the given SSH session."
  [session args]
  (try
    (let [command (str/join " " args)]
      (log/debug (str "Executing command: " command))
      (let [wrapped-command (str "echo \"___START_OUT___\"; " command "; echo \"___END_OUT___\"")
            {exit :exit :as response} (ssh/ssh session {:in wrapped-command})]
        (log-response response)
        (-> response
            (update :out (comp output unix-eol))
            (merge (if (zero? exit)
                     {:status :ok :ok true}
                     {:status :failure :ok false})))))
    (catch com.jcraft.jsch.JSchException ex
      (log/error ex)
      {:status :exception :ok false :exception ex})))

(defn session
  "Returns a fresh SSH session."
  ([host username password]
   (ssh/session (ssh/ssh-agent {}) host {:username username
                                         :password password
                                         :strict-host-key-checking :no}))
  ([{:keys [:shelf/host :shelf/username :shelf/password]}]
   (session host username password)))

;; single-session shell
(defrecord SingleSessionBasicSSH [session]
  Shell
  (execute [this args]
    (execute-in-session session args)))

(defn single-session-basic-ssh
  [session]
  (SingleSessionBasicSSH. session))

;; multi-session shell
(defrecord MultiSessionBasicSSH [host username password]
  Shell
  (execute [this args]
    (let [session (session host username (crypto/decrypt password "ssh"))]
      (try
        (execute-in-session session args)
        (finally (ssh/disconnect session))))))

;; local
(defrecord LocalShell []
  Shell
  (execute [this args]
    (log/debug (str "Executing command: " (str/join " " args)))
    (let [{exit :exit :as response} (apply sh/sh args)]
      (log-response response)
      (merge response (if (zero? exit)
                        {:status :ok :ok true}
                        {:status :failure :ok false})))))

(defn local-sh [_] (LocalShell.))

(defn multi-session-basic-ssh
  [{:keys [:shelf/host :shelf/username :shelf/password]}]
  (MultiSessionBasicSSH. host username (crypto/encrypt password "ssh")))

;; rest
(defn ssh-config [type host username password]
  {:shelf/type type
   :shelf/host host
   :shelf/username username
   :shelf/password password})

(defn exec [shell args] (execute shell args))

(defn shell
  [config]
  (if-let [error (s/explain-data :shelf/config config)]
    (throw (ex-info "Invalid shelf config." error))
    (let [constructor (case (:shelf/type config)
                        "local" local-sh
                        "multi-session-basic-ssh" multi-session-basic-ssh
                        "single-session-basic-ssh" single-session-basic-ssh)]
      (constructor config))))

(defmacro with-ssh
  "Executes body with session and shell bound to a fresh SSH session and a single-session shell respectively."
  [config & body]
  `(let [session# (session ~config)]
     (try
       (let [~'shell (single-session-basic-ssh session#)]
           (when-not (ssh/connected? session#)
             (ssh/connect session#))
           ~@body)
       (finally
        (ssh/disconnect session#)))))
