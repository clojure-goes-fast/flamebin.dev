(ns flamebin.core
  (:require [flamebin.db :as db]
            [flamebin.processing :as proc]
            [flamebin.dto :as dto]
            [flamebin.render :as render]
            [flamebin.storage :as storage]))

(defn save-profile [collapsed-profile-file profile-id owner]
  (let [filename (format "%s.cpf" profile-id)]
    (storage/save-file (proc/full-collapsed-profile->cpf-byte-array collapsed-profile-file)
                       filename)
    (db/insert-profile (dto/->Profile profile-id filename "cpu" nil owner))))

(defn read-profile [profile-id]
  (let [f (db/find-cpf-file profile-id)]
    (proc/read-compressed-profile (storage/get-file f))))

(defn render-profile [profile-id]
  (render/render-html-flamegraph (read-profile profile-id) {}))

(defn list-profiles []
  (db/list-profiles))

(comment
  (save-profile "/Users/alex/clojure/clj-async-profiler/exp-samples.txt" (nid) "me")

  (time+ (read-profile "jgnXp3Vfcm"))

  (time+ (read-profile "Q4QNFfaxuJ"))

  (read-profile "aXYxNb6eWY")

  (user/debugging-tools)

  (prof/profile {:event :alloc}
    (time+ 10000
      (count (render-profile "Q4QNFfaxuJ")))))
