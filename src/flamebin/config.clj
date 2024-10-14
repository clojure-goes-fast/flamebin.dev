(ns flamebin.config
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            malli.experimental.time.transform
            [malli.transform :as mt]
            [omniconf.core :as cfg]
            [taoensso.timbre :as log]))

(defn init-config []
  (cfg/set-logging-fn (fn [& args] (log/info (str/join " " args))))
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
                  :default #(when (= (cfg/get :env) :local) "test.db")}}}})
  (cfg/verify))

(def global-transformer
  (mt/transformer
   mt/string-transformer
   malli.experimental.time.transform/time-transformer))
