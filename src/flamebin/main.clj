(ns flamebin.main
  (:require [flamebin.init :as init]
            [flamebin.config :as config]
            [flamebin.db :as db]
            [flamebin.web :as web]
            [omniconf.core :as cfg]))

(defn -main [& args]
  (flamebin.config/init-config args)
  (db/migrate)
  (web/start-server (cfg/get :server :port)))
