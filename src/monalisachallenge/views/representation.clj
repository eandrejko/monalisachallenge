(ns monalisachallenge.views.representation
  (:use noir.core
        noir.request
        hiccup.core
        hiccup.page
        monalisachallenge.rendering
        cheshire.core)
  (:require [noir.response :as resp]
            [monalisachallenge.models.representation :as representation]
            [monalisachallenge.representation :as r]))

(def cred {:access-key (System/getenv "AWS_ACCESS"), :secret-key (System/getenv "AWS_SECRET")})

(defpage [:post "/representation/"] {}
  (let [repr (slurp (:body (ring-request)))
        repr (-> repr
                 (read-string))]
    (if (r/valid-representation? (:representation repr))
      (do
        (representation/save! repr)
        (resp/content-type "text/json" (pr-str repr)))
      (resp/content-type "text/plain" "Invalid representation"))))

(defpage "/representation/" {}
  (let [repr (representation/best-representation)]
    (resp/content-type "text/plain" (pr-str repr))))
