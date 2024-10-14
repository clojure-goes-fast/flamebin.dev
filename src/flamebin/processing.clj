(ns flamebin.processing
  (:require [clj-async-profiler.post-processing :as pp]
            [taoensso.nippy :as nippy]
            ))

(def ^:private nippy-compressor nippy/zstd-compressor)

(defn full-collapsed-profile->cpf-byte-array [full-profile-file]
  (nippy/freeze (pp/read-raw-profile-file-to-compact-profile full-profile-file)
                {:compressor nippy-compressor}))

(defn read-compressed-profile [source-file]
  (nippy/thaw-from-file source-file))

(comment
 (full-collapsed-profile->cpf-byte-array "/Users/alex/clojure/clj-async-profiler/exp-samples.txt")

 (def -compact (pp/read-raw-profile-file-to-compact-profile "/Users/alex/clojure/clj-async-profiler/exp-samples.txt"))

 (def -compact2 (update -compact :stacks
                        (fn [frames]
                          (mapv (fn [[a b]]
                                  [(long-array a) b])
                                frames))))


 (count (apply concat (map first (:stacks -compact)))) (* 4 117590)

 (sort > (map count (map first (:stacks -compact))))

 #_(user/debugging-tools)

 (mm/measure (nippy/freeze (mapv first (:stacks -compact2)) {:compressor nippy/zstd-compressor}))
 (mm/measure (:stacks -compact2))

 (nippy/freeze (:id->frame -compact) {:compressor nil})

 #_(count (pp/read-raw-profile-file-to-compact-profile "/Users/alex/clojure/clj-async-profiler/diff-a.txt"))
 )
