(ns aiba.oz-server
  (:gen-class)
  (:require [clojure.string :as str]
            [clojure.tools.cli :refer [parse-opts]]
            [cognitect.transit :as transit]
            [oz.core :as oz]
            oz.server
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

;; main ————————————————————————————————————————————————————————————————————————————

(def cli-options
  [["-p" "--plot-port PORT" "Port number for oz plot server"
    :default oz.server/default-port
    :parse-fn #(Integer/parseInt %)]
   ["-c" "--control-port PORT" "Port number for control server"
    :default (inc oz.server/default-port)
    :parse-fn #(Integer/parseInt %)]
   ["-h" "--help"]])

(comment
  (parse-opts ["--help"] cli-options)
  (println (:summary (parse-opts ["--help"] cli-options)))
  (parse-opts ["--plot-port" "2000"] cli-options)
  (parse-opts ["--blah" "2000"] cli-options)
  (parse-opts ["-p2000"] cli-options)
  (parse-opts ["-p2000" "--control-port" "3000"] cli-options)
  )

(defn main [{:keys [plot-port control-port]}]
  (oz/start-server! plot-port)
  (println "oz-server running on port" plot-port)
  (start-control-server! control-port)
  (println "control-server running on port" control-port))

(defn -main [& args]
  (let [{:keys [options errors summary]} (parse-opts args cli-options)]
    (cond
      errors          (println (str/join "\n" errors) "\n\nUSAGE:\n\n" summary)
      (:help options) (println errors "USAGE:\n\n" summary)
      :else           (main options))))
