(ns flamebin.dto
  (:require [clojure.test.check.generators :as gen]
            [flamebin.util :refer [new-id raise valid-id? gen-invoke]]
            [malli.core :as m]
            [malli.experimental.lite :as mlite]
            [malli.experimental.time.transform]
            [malli.transform :as mt]
            flamebin.init
            [flamebin.config]
            [migratus.core :as migratus]
            [next.jdbc :as jdbc]
            [next.jdbc.sql :as sql-helpers]
            [omniconf.core :as cfg]
            [taoensso.timbre :as log])
  (:import java.time.Instant))

;;;; Profile

(def Profile
  (mlite/schema
   {:id [:and {:gen/gen (gen-invoke new-id)}
         :string
         [:fn valid-id?]]
    :file_path [:and {:gen/fmap #(str % ".cpf")} :string]
    :profile_type :keyword
    :upload_ts [:and {:gen/gen (gen/fmap Instant/ofEpochSecond
                                         (gen/choose 1500000000 1700000000))}
                :time/instant]
    :sample_count [:maybe nat-int?]
    :owner [:maybe string?]}))

(defn ->Profile
  ([id file_path profile_type sample_count owner upload_ts]
   (let [obj {:id id, :file_path file_path, :profile_type profile_type,
              :upload_ts upload_ts, :sample_count sample_count, :owner owner}]
     (m/coerce Profile obj flamebin.config/global-transformer)))
  ([id file_path profile_type sample_count owner]
   (->Profile id file_path profile_type sample_count owner (Instant/now))))
