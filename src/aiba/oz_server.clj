(ns aiba.oz-server
  (:require [cognitect.transit :as transit]
            [oz.core :as oz]
            [ring.adapter.jetty :refer [run-jetty]]))

(defn read-transit [^String x]
  (-> x
      (.getBytes "UTF-8")
      (java.io.ByteArrayInputStream.)
      (transit/reader :json)
      (transit/read)))

(defn write-transit [x]
  (let [out (java.io.ByteArrayOutputStream.)]
    (transit/write (transit/writer out :json) x)
    (.toString out)))

(defn control-handler [req]
  (let [{:keys [method args]} (-> req :body slurp read-transit)
        f (case method
            :view! oz/view!)]
    {:status 200
     :headers {"Content-Type" "application/transit+json; charset=utf-8"}
     :body (write-transit (apply f args))}))

(defonce *control-server (atom nil))

(defn start-control-server! [port]
  (when-let [s @*control-server]
    (s))
  (reset! *control-server
          (run-jetty #'control-handler {:port port, :join? false})))

(def oz-server-port 7878)
(def control-port 7879)

(defn -main [& args]
  (oz/start-server! oz-server-port)
  (println "oz-server running on port" oz-server-port)
  (start-control-server! control-port)
  (println "control-server running on port" control-port))

;; testing —————————————————————————————————————————————————————————————————————————

(defn play-data [& names]
  (for [n names
        i (range 200)]
    {:time i :item n :quantity (+ (Math/pow (* i (count n)) 0.8) (rand-int (count n)))}))

(def line-plot
  {:data {:values (play-data "monkey" "slipper" "broom")}
   :encoding {:x {:field "time" :type "quantitative"}
              :y {:field "quantity" :type "quantitative"}
              :color {:field "item" :type "nominal"}}
   :mark "line"})

(comment
  (-main)
  (require '[clj-http.client :as http])
  (:body (http/post (str "http://localhost:" control-port "/")
                    {:body (write-transit {:method :view!
                                           :args [line-plot]})}))
  )
