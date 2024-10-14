(ns flamebin.config
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            malli.experimental.time.transform
            [malli.transform :as mt]
            [omniconf.core :as cfg]
            [taoensso.timbre :as log]))

(cfg/define
  {:env {:type :keyword
         :one-of [:local :qa :prod]
         :default :local}

   :storage {:nested
             {:kind {:type :keyword
                     :default :disk}
              :path {:type :directory
                     :required true
                     :verifier cfg/verify-file-exists
                     :default #(when (= (cfg/get :env) :local)
                                 (io/file "storage/"))}}}
   :db {:nested
        {:path {:type :string
                :required true
                :default #(when (= (cfg/get :env) :local) "test.db")}}}

   :server {:nested
            {:port {:type :number
                    :required true
                    :default #(when (= (cfg/get :env) :local) 8086)}}}})

(cfg/set-logging-fn (fn [& args] (log/info (str/join " " args))))

(defn init-config [args]
  (cfg/populate-from-cmd args)
  (cfg/populate-from-env)
  (cfg/verify))

(def global-transformer
  (mt/transformer
   mt/string-transformer
   malli.experimental.time.transform/time-transformer))
