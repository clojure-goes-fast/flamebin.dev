(ns flamebin.storage
  (:require [clojure.java.io :as io])
  (:import java.io.ByteArrayInputStream)
  )

(def root-dir (doto (io/file "storage") .mkdirs))

(defn save-file [content filename]
  (with-open [f (io/output-stream (io/file root-dir filename))]
    (io/copy content f)))

(defn get-cpf-file [profile-id]
  (io/file root-dir (format "%s.cpf" profile-id)))

(defn get-file [path]
  (io/file root-dir path))
