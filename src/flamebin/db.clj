(ns flamebin.db
  (:require [clojure.test.check.generators :as gen]
            [flamebin.util :refer [new-id raise valid-id? gen-invoke]]
            [malli.core :as m]
            [malli.experimental.lite :as mlite]
            [malli.experimental.time.transform]
            [malli.transform :as mt]
            flamebin.init
            [migratus.core :as migratus]
            [next.jdbc :as jdbc]
            [next.jdbc.sql :as sql-helpers]
            [omniconf.core :as cfg]
            [taoensso.timbre :as log])
  (:import java.time.Instant))

;;;; Preparation

flamebin.init/dont-remove-this-namespace

(defn- db-options []
  {:dbtype "sqlite"
   :dbname (cfg/get :db :path)})

(defn- migratus-config []
  {:store                :database
   :migration-dir        "migrations/"
   :init-script          "init.sql"
   :init-in-transaction? false
   :db                   (db-options)})

(defn migrate []
  (migratus/init (migratus-config))
  (migratus/migrate (migratus-config)))

#_(migrate)


;;;; DB interaction

(defn now []
  (.toInstant (java.util.Date.)))

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

#_((requiring-resolve 'malli.generator/sample) Profile)

(def ^:private db-transformer
  (mt/transformer
   mt/string-transformer
   malli.experimental.time.transform/time-transformer))

(defn insert-profile [profile]
  (let [{:keys [id file_path profile_type sample_count owner upload_ts]}
        (m/coerce Profile (update profile :upload_ts #(or % (now))) db-transformer)]
    (log/infof "Inserting profile %s from %s" id owner)
    (sql-helpers/insert! (db-options) :profile
                         {:id id
                          :file_path file_path
                          :profile_type (name profile_type)
                          :upload_ts (str upload_ts)
                          :sample_count sample_count
                          :owner owner})))

(defn find-cpf-file [profile-id]
  (let [q ["SELECT file_path FROM profile WHERE id = ?" profile-id]]
    (if-some [row (jdbc/execute-one! (db-options) q)]
      (:profile/file_path row)
      (let [msg (format "Profile with ID '%s' not found." profile-id)]
        (log/error msg)
        (raise 404 msg)))))

(defn list-profiles []
  (->> (jdbc/execute! (db-options) ["SELECT id, file_path, profile_type, sample_count, owner, upload_ts FROM profile"])
       (mapv #(m/coerce Profile (update-keys % (comp keyword name)) db-transformer))))

(defn get-profile [profile-id]
  (let [q ["SELECT id, file_path, profile_type, sample_count, owner, upload_ts FROM profile WHERE id = ?" profile-id]]
    (if-some [row (update-keys (jdbc/execute-one! (db-options) q) (comp keyword name))]
      (m/coerce Profile row db-transformer)
      (let [msg (format "Profile with ID '%s' not found." profile-id)]
        (log/error msg)
        (raise 404 msg)))))

(defn clear-db []
  (.delete (clojure.java.io/file (cfg/get :db :path)))
  (migrate))

(comment
  (clear-db)
  (insert-profile {:id (new-id) :file_path "no.txt" :profile_type :alloc :sample_count 100 :owner "me"}  )
  (insert-profile {:id (new-id) :file_path "nilsamples" :profile_type "noexist" :sample_count nil :owner "me"})
  (find-cpf-file "xDRA4dpWFM")
  (let [p (malli.generator/generate Profile)]
    (insert-profile p)
    (= p (first (list-profiles))))
  (get-profile "cBkfhWcMPL")
  )
