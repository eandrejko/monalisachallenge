(ns monalisachallenge.views.representation
  (:use noir.core
        noir.request
        hiccup.core
        hiccup.page
        monalisachallenge.rendering
        cheshire.core)
  (:require [noir.response :as resp]
            [monalisachallenge.models.representation :as representation]))

(def cred {:access-key (System/getenv "AWS_ACCESS"), :secret-key (System/getenv "AWS_SECRET")})

(defpage [:post "/representation/"] {}
  (let [repr (slurp (:body (ring-request)))
        repr (-> repr
                 (read-string)
                 (representation/save!))]
    (resp/content-type "text/plain" (pr-str {:representation repr}))))

(defpage "/representation/" {}
  (let [repr (representation/best-representation)]
    (resp/content-type "text/plain" (pr-str {:representation repr}))))