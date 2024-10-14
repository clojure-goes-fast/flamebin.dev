(ns flamebin.web
  (:require [flamebin.core :as core]
            [flamebin.util :refer [new-id valid-id?]]
            [muuntaja.core :as content]
            [omniconf.core :as cfg]
            [taoensso.timbre :as log]
            [muuntaja.middleware :as content.mw]
            [org.httpkit.server :as server]
            reitit.coercion.malli
            [flamebin.web.pages :as pages]
            [reitit.ring :as ring]
            [reitit.ring.coercion :as ring-coercion]
            [reitit.ring.middleware.parameters :refer [parameters-middleware]])
  (:import clojure.lang.ExceptionInfo
           java.util.zip.GZIPInputStream))

;; Middleware

(defn resp
  ([body] (resp 200 body))
  ([status body] {:status 200, :body body}))

(def ^:private content-middleware-config
  (content/create
   (update content/default-options :formats select-keys ["application/json"])))

(defn wrap-gzip-request [handler]
  (fn [request]
    (if (= (get-in request [:headers "content-encoding"]) "gzip")
      (with-open [gzip-body (GZIPInputStream. (:body request))]
        (handler (assoc request :body gzip-body)))
      (handler request))))

(defn wrap-ignore-form-params [handler]
  (fn [request]
    ;; Hack for wrap-params to ignore :body and not parse it to get form-params.
    (handler (update request :form-params #(or % {})))))

(defn wrap-exceptions [handler]
  (fn [request]
    (try (handler request)
         (catch ExceptionInfo ex
           (let [{:keys [http-code]} (ex-data ex)]
             {:status (or http-code 500)
              :body (ex-message ex)})))))

(defn $upload-collapsed [req]
  (let [id (new-id)]
    (core/save-profile (:body req) id "me")
    {:status 200
     :body (str "Success! " id ", owner: " "me")}))

(defn $upload-collapsed-file [req]
  (def -req req)
  (let [id (new-id)]
    (core/save-profile (:body req) id "me")
    {:status 200
     :body (str "Success! " id ", owner: " "me")}))

(defn $page-upload-file [req]
  (resp (pages/upload-page)))

(defn $page-list-profiles [req]
  (resp (pages/index-page)))

(defn $list-profiles [req]
  {:status 200
   :body (flamebin.db/list-profiles)})

(defn $render-profile [{:keys [path-params]}]
  (let [{:keys [profile-id]} path-params
        rendered (core/render-profile profile-id)] ;; "Q4QNFfaxuJ"
    {:headers {"content-type" "text/html"}
     :body rendered}))

(def app
  (ring/ring-handler
   (ring/router
    [ ;; HTML
     ["" {}
      ["/:profile-id" {:get {:handler #'$render-profile
                             :coercion reitit.coercion.malli/coercion
                             :parameters {:path {:profile-id
                                                 [:and string? [:fn valid-id?]]}}}}]
      ["/profiles/upload" {:get {:handler #'$page-upload-file}}]
      ["/" {:get {:handler #'$page-list-profiles}}]
      ["/profiles/upload-collapsed-file" {:post {:handler #'$upload-collapsed-file}}]]

     ;; API
     ["/api/v1" {}
      ["/profiles/upload-collapsed" {:post {:handler #'$upload-collapsed}}]
      ["/profiles" {:get {:handler #'$list-profiles
                          :coercion reitit.coercion.malli/coercion}}]
      ["/render" {:get {:handler #'$render-profile}}]
      #_["/heartbeat" {:post {:handler #'$heartbeat}}]]

     ;; ;; Protected routes
     ;; ["" {:middleware [wrap-require-authenticated]}
     ;;  ["/add-person" {:get {:handler #'$add-person-page}
     ;;                      :post {:handler  #'$add-person-action}}]
     ;;  ["/person/:id/update-key" {:post {:handler #'$update-key}}]
     ;;  ["/person/:id/flip-forbidden-status" {:get {:handler #'$flip-forbidden-status}}]
     ;;  ["/person/:id/manual-move" {:get {:handler #'$manual-move}}]]
     ]
    {:data {:middleware [;; Needed for coercion to work.
                         ring-coercion/coerce-exceptions-middleware
                         ring-coercion/coerce-request-middleware]}})
   (ring/redirect-trailing-slash-handler)
   {:middleware [[wrap-exceptions]
                 [content.mw/wrap-format content-middleware-config]
                 [wrap-gzip-request]
                 [wrap-ignore-form-params]
                 [parameters-middleware]
                 ;; [wrap-session {:store cookie-jar}]
                 ;; [buddy.mw/wrap-authentication auth-backend]
                 ;; [buddy.mw/wrap-authorization auth-backend]
                 ;; [wrap-logging]
                 ]}))

(def ^:private server (atom nil))

(defn start-server [port]
  (when @server (@server))
  (log/infof "Starting web server on port %d" port)
  (reset! server (server/run-server #'app {:port port})))

#_(start-server (cfg/get :server :port))
