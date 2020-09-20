(ns aiba.oz-server-test
  (:require [aiba.oz-server :as oz-server]
            [clj-http.client :as http]))

;; testing —————————————————————————————————————————————————————————————————————————

(defn play-data [& names]
  (for [n names
        i (range 20)]
    {:time i :item n :quantity (+ (Math/pow (* i (count n)) 0.8) (rand-int (count n)))}))

(defn line-plot []
  {:data {:values (play-data "monkey" "slipper" "broom")}
   :encoding {:x {:field "time" :type "quantitative"}
              :y {:field "quantity" :type "quantitative"}
              :color {:field "item" :type "nominal"}}
   :mark "line"
   :width 640
   :height 480})

(def control-port 7879)

(defn remote! [method & args]
  (oz-server/read-transit
   (:body (http/post (str "http://localhost:" control-port "/")
                     {:body (oz-server/write-transit {:method method
                                                      :args args})}))))

(comment
  (oz-server/-main "--control-port" "7879")
  (remote! :view! (line-plot))
  )
