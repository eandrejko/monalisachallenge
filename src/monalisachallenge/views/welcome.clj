(ns monalisachallenge.views.welcome
  (:require [monalisachallenge.views.common :as common]
            [monalisachallenge.models.representation :as repr]
            [noir.response :as response])
  (:use [noir.core :only [defpage]]))

(defpage "/" []
  (response/set-headers
   {"Cache-Control" "public, max-age=300"}
   (common/layout
    [:div.hero-unit
     [:h1 "The Mona Lisa Challenge"]
     [:p "Help find the solution: How can the Mona Lisa be optimally represented using only "
      [:em [:b
            "fifty translucent polygons"]]
      "?"]
     [:p
      [:a.btn.btn-primary.btn-large {:href "/contribute"} "Join the Search »"]]]
    [:div.row
     [:div.span4
      [:h2 "The Project"]
      "The Mona Lisa problem is a toy problem in the domain of optimization theory. There are many well known algorithms to search for a solution. This project uses a distributed evolutionary algorithm, implemented in the browser."
      [:p [:a.btn {:href "/about"} "How it works »"]]]
     [:div.span4
      [:h2 "The Results"]
      [:img {:src (repr/url-of-best) :style "float: left; width: 80px;"}]
      [:img {:src "/img/mona-lisa-head-192.jpg" :style "float: left; width: 80px; margin-left: 10px;"}]
      [:p {:style "clear: both;"} "The best solution found so far after searching "
       100
       " possiblities"]
      [:p [:a.btn {:href "/results"} "View Results »"]]]
     [:div.span4
      [:h2 "Contribute"]
      "Contribute computational power to the search for the solution:"
      [:ul
       [:li "Implement your own search algorithms and submit your results"]
       [:li "Run the distributed computational client in your browser"]]
      [:p [:a.btn {:href "/contribute"} "Contribute a solution »"]]]])))

(defpage "/contribute" []
  (common/layout
   [:h1 "Join the Search for the Mona Lisa"]
   [:div.row
    [:div.span3
     [:p "Search directly in the browser using a Javascript client. The search will run as long as this browser window is left open, so leave this brower window open as long as possible to make the largest contribution."]
     [:a.btn.btn-primary.btn-large {:href "#" :id "start"} "Start the Search »"]]
    [:div.span9
     [:ul.thumbnails
      [:li.span3
       [:div.thumbnail
        (map (fn [z]
              [:img {:src z :style "width:20%; margin: 2.4% 2.8%; float: left;"}])
             (repr/history-of-search (repr/id-of-best) 17))
        [:p {:style "clear:both;"} "History of Search"]]]
      [:li.span3
       [:div.thumbnail
        [:div#representation {:width "192" :height "239" :style "color: #888888; width: 192px; position: relative;"}]
        [:p "Search Target"]
        [:p [:div#info]]]]
      [:li.span3
       [:div.thumbnail.span3
        [:div#reference
         {:width "192" :height "239"}]
        [:p "Mona Lisa"]]]]]]
   [:div.row
    [:h1 "Implement your own search algorithm"]]
   (noir.cljs.core/include-scripts)
   [:script {:type "text/javascript"} "monalisachallenge.client.init();$(\"#start\").click(monalisachallenge.client.start);$(\"#start\").click(function(e){$(\"#start\").text(\"Searching\").addClass(\"disabled\")})"]))
