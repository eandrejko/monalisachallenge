(ns monalisachallenge.views.common
  (:use [noir.core :only [defpartial]]
        [hiccup.page :only [include-css include-js html5]]))

(defpartial layout [& content]
            (html5
              [:head
               [:title "The Mona Lisa Challenge"]
               (include-css "/css/bootstrap.css")
               (include-css "/css/bootstrap-responsive.css")
               [:style "body { padding-top: 60px; }"]]
              [:body
               (list
                (include-js "http://ajax.googleapis.com/ajax/libs/jquery/1.7.1/jquery.min.js")
                (include-js "/js/bootstrap.min.js")
                [:div.navbar.navbar-fixed-top {"data-toggle" "collapse" "data-target" ".nav-collapse"}
                 [:div.navbar-inner
                  [:div.container
                   [:a.btn.btn-navbar
                    [:span.icon-bar]]
                   [:a.brand
                    [:img {:src "/img/mona-lisa-head-192.jpg" :style "width: 20px; margin-right: 5px;"}]
                    "Mona Lisa Challenge"]
                   [:div.nav-collapse
                    [:ul.nav
                     [:li.active
                      [:a {"href" "/"} "Home"]]
                     [:li
                      [:a {"href" "/about"} "About"]]
                     [:li
                      [:a {"href" "/contribute"} "Contribute"]]
                     [:li
                      [:a {"href" "/results"} "Results"]]]]]]]
                [:div.container content])]))
