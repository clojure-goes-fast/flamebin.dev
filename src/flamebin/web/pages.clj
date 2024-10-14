(ns flamebin.web.pages
  (:require [hiccup2.core :as h]
            [flamebin.core :as core]
            [hiccup.page :refer [include-js]]
            [hiccup.form :as form]
            [clojure.java.io :as io]))

(defn upload-page []
  (str
   (h/html
       [:html
        [:head
         [:title "Gzipped File Upload"]]
        [:body
         [:h1 "Gzipped File Upload"]
         (form/form-to {:enctype "multipart/form-data"
                        :id "uploadForm"}
                       [:post "/profiles/upload-collapsed-file"]
                       (form/file-upload {:id "fileInput"} "file")
                       (form/submit-button "Upload"))
         [:div#status]
         [:script (h/raw (slurp (io/resource "site/upload.js")))]]])))

(defn index-page []
  (str
   (h/html
       [:html
        [:head
         [:title "flamebin.dev"]]
        [:body
         [:h1 "welcome to flamebin"]
         [:p [:a {:href "/profiles/upload"} "Upload another"]]
         [:ul
          (for [p (core/list-profiles)]
            [:li [:a {:href (format "http://localhost:8086/%s" (:id p))} (:id p)]])]]])))
