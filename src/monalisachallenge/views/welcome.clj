(ns monalisachallenge.views.welcome
  (:require [monalisachallenge.views.common :as common])
  (:use [noir.core :only [defpage]]))

(defpage "/" []
  (common/layout
   [:h1 "Welcome to Noir Bootstrap"]
   [:p (list
        "A combination of "
        [:a {"href" "http://webnoir.org"} "Noir"]
        " and "
        [:a {"href" "http://twitter.github.com/bootstrap/"} "Twitter Bootstrap"]
        "."
        [:br]
        "Use this as a way to quick start any new project.")]))

(defpage "/search" []
  (common/layout
   [:h1 "Evolve Mona Lisa"]
   [:div.row
    [:div.span6
     [:div#representation {:width "192" :height "239" :style "color: #888888; margin-left: auto; margin-right: 0px; width: 192px; position: relative;"}
      [:div#info {:style "position:absolute; right:0; bottom:0; background-color:rgba(0,0,0,0.5); color:#fff; z-index:10; padding: 0 10px;"}]]]
    [:div.span6
     ;; [:img {:src "/img/mona-lisa-head-192.jpg" :syle "margin-left: 0px;"}]
     [:div#reference {:width "192" :height "239" :style "margin-left: 0px; width: 192px;"}]]]))
