(ns monalisachallenge.server
  (:require [noir.server :as server]
            [noir.cljs.core :as cljs]))

(server/load-views "src/noir_bootstrap/views/")

(def cljs-options {:simple {:libs ["externs/loss.js"]}})

(defn -main [& m]
  (let [mode (keyword (or (first m) :dev))
        port (Integer. (get (System/getenv) "PORT" "8080"))]
    (cljs/start mode cljs-options)
    (server/start port {:mode mode
                        :ns 'monalisachallenge})))

