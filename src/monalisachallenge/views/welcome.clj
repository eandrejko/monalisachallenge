(ns monalisachallenge.views.welcome
  (:require [monalisachallenge.views.common :as common])
  (:use [noir.core :only [defpage]]
        [noir.response]))

(defpage "/" []
  (set-headers
   {"Cache-Control" "public, max-age=300"}
   (common/layout
    [:div.hero-unit
     [:h1 "The Mona Lisa Challenge"]
     [:p "Help find the solution: How can the Mona Lisa be optimally represented using only "
      [:em
       "fifty tranluscent polygons"]
      "?"]
     [:p
      [:a.btn.btn-primary.btn-large {:href "/search"} "Join the Search »"]]]
    [:div.row
     [:div.span4
      [:h2 "The Project"]
      "The Mona Lisa problem is a toy problem in the domain of optimization theory. There are many well known algorithms to search for a solution. This project uses a distributed evolutionary algorithm, implemented in the browser."
      [:p [:a.btn {:href "/details"} "How it works »"]]]
     [:div.span4
      [:h2 "The Results"]
      [:img {:src "/img/mona-lisa-head-192.jpg" :style "float: left; width: 80px;"}]
      [:img {:src "/img/mona-lisa-head-192.jpg" :style "float: left; width: 80px; margin-left: 10px;"}]
      [:p {:style "clear: both;"} "The best solution found so far after searching."]
      [:p [:a.btn {:href "/results"} "View Results »"]]]
     [:div.span4
      [:h2 "Contribute"]
      "Contribute computational power to the search for the solution:"
      [:ul
       [:li "Implement your own solutions and submit your results"]
       [:li "Run the distributed computational client in your browser"]]
      [:p [:a.btn {:href "/contribute"} "Contribute a solution »"]]]])))

(defpage "/search" []
  (common/layout
   [:h1 "Search for Mona Lisa"]
   [:div.row
    [:div.span6
     [:div#representation {:width "192" :height "239" :style "color: #888888; margin-left: auto; margin-right: 0px; width: 192px; position: relative;"}
      [:div#info {:style "position:absolute; right:0; bottom:0; background-color:rgba(0,0,0,0.5); color:#fff; z-index:10; padding: 0 10px;"}]]]
    [:div.span6
     [:div#reference
      {:width "192" :height "239" :style "margin-left: 0px; width: 192px;"}]]]
   (noir.cljs.core/include-scripts)
   [:script {:type "text/javascript"} "monalisachallenge.client.start();"]))
