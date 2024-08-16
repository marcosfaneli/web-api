(ns web-api.core
  (:require [ring.adapter.jetty :as jetty]
            [compojure.core :refer :all]
            [clojure.data.json :as json]))

(def users
  [{:name "alice" :department "HR"}
   {:name "bob" :department "IT"}])

(def defaults
  {:security {:anti-forgery true}})

(def site-defaults
  (-> defaults
    (assoc-in [:security :anti-forgery] false)))

(defn wrap-defaults [handler site-defaults]
  (let [defaults (merge defaults site-defaults)]
    (fn [request]
      (handler (assoc request :site-defaults defaults)))))

(defn handler-hello-world [request]
  (let [value (-> request :params :value)]
    {:status  200
     :body    {:message (str "Hello, " value "!")}}))

(defn handler-users [request]
  {:status  200
   :body    users})

(defn wrap-authentication [handler]
  (fn [request]
    (println "Authentication")
    (if-let [token (-> request :headers (get "authorization"))]
      (handler request)
      {:status  401
       :body    {:message "Unauthorized"}})))

(defroutes app-routes
  (GET "/" [] handler-hello-world)
  (GET "/hello/:value" [] handler-hello-world)
  (GET "/users" [] (-> handler-users wrap-authentication)))

(defn wrap-json-response [handler]
  (fn [request]
    (let [response (handler request)]
      (println "JSON Response")
      (assoc response
        :body (json/write-str (:body response))
        :headers (assoc (:headers response) "Content-Type" "application/json")))))

(def app
  (-> app-routes
      (wrap-defaults site-defaults)
      wrap-json-response))

(defn -main [& args]
  (jetty/run-jetty app {:port 3000}))
