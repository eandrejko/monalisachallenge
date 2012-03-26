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
                [:div.navbar.navbar-fixed-top {"data-toggle" "collapse" "data-target" ".nav-collapse"}
                 [:div.navbar-inner
                  [:div.container
                   [:a.btn.btn-navbar
                    [:span.icon-bar]]
                  [:a.brand "Mona Lisa Challenge"]
                   [:div.nav-collapse
                    [:ul.nav
                     [:li.active
                      [:a {"href" "#"} "Home"]]]]]]]
                [:div.container content] 
                (include-js "http://ajax.googleapis.com/ajax/libs/jquery/1.7.1/jquery.min.js")
                (include-js "/js/bootstrap.min.js")
                (noir.cljs.core/include-scripts))]))
