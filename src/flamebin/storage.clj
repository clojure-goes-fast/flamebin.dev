(ns flamebin.storage
  (:require [clojure.java.io :as io]
            [omniconf.core :as cfg])
  (:import java.io.ByteArrayInputStream))

(defn root-dir []
  (cfg/get :storage :path))

(defn save-file [content filename]
  (with-open [f (io/output-stream (io/file (root-dir) filename))]
    (io/copy content f)))

(defn get-cpf-file [profile-id]
  (io/file (root-dir) (format "%s.cpf" profile-id)))

(defn get-file [path]
  (io/file (root-dir) path))
