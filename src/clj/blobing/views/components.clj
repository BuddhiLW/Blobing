(ns blobing.views.components
  "Small hiccup building blocks shared by the page views. Every function is
  pure: data in, hiccup out."
  (:require [hiccup2.core :as h])
  (:import (java.time ZoneId)
           (java.time.format DateTimeFormatter)
           (java.util Date Locale)))

(def ^:private long-date (DateTimeFormatter/ofPattern "MMMM d, yyyy" Locale/ENGLISH))
(def ^:private iso-date DateTimeFormatter/ISO_LOCAL_DATE)

(defn- local-date [^Date date]
  (-> date .toInstant (.atZone (ZoneId/systemDefault)) .toLocalDate))

(defn format-date
  "Formats a java.util.Date with a DateTimeFormatter pattern string."
  [^Date date pattern]
  (.format (DateTimeFormatter/ofPattern pattern Locale/ENGLISH) (local-date date)))

(defn date
  "A <time> element. `pattern` defaults to a long English date."
  ([d] (date d nil))
  ([d pattern]
   (when d
     [:time {:datetime (.format iso-date (local-date d))}
      (if pattern (format-date d pattern) (.format long-date (local-date d)))])))

(defn raw
  "Pre-rendered HTML (article bodies, generated TOCs) passed through unescaped."
  [html]
  (when html (h/raw html)))

(defn tag-list [tags]
  (when (seq tags)
    [:ul.tag-list {:aria-label "Tags"}
     (for [{:keys [uri name]} tags]
       [:li [:a.tag {:href uri} name]])]))

(defn outline-nav
  "Section index from `cryogen.content/outline`: a sticky rail beside the
  article on wide screens, a collapsible block above the text on narrow
  ones (blobing.site highlights the section being read). Rendered only for
  pages with at least two sections."
  [outline]
  (when (next outline)
    [:aside.outline {:aria-label "On this page"}
     [:details {:open true}
      [:summary "On this page"]
      [:ol
       (for [{:keys [id level text]} outline]
         [:li {:class (str "outline-h" level)}
          [:a {:href (str "#" id)} text]])]]]))

(defn prev-next [{:keys [prev next]}]
  (when (or prev next)
    [:nav.prev-next {:aria-label "More posts"}
     (when prev
       [:a.prev {:href (:uri prev) :rel "prev"}
        [:span.label "← Newer"] [:span.title (:title prev)]])
     (when next
       [:a.next {:href (:uri next) :rel "next"}
        [:span.label "Older →"] [:span.title (:title next)]])]))

(defn comments
  "Mount point for the self-hosted Remark42 comments (threads, replies,
  social and Keycloak logins; sanitized server-side). Rendered only once
  `:comments {:host ...}` is configured, so an unconfigured site shows
  nothing rather than a broken box. blobing.site loads the embed script."
  [{:keys [host site-id locale]} page-url]
  (when (seq host)
    [:section#comments.comments {:aria-labelledby "comments-title"}
     [:h2#comments-title "Comments"]
     [:p.comments-note "Sign in with Google, GitHub, Facebook or your account to comment and reply."]
     [:div#remark42 {:data-host    host
                     :data-site-id (or site-id "blobing")
                     :data-url     page-url
                     :data-locale  (or locale "en")}]
     [:noscript "Comments need JavaScript."]]))

(defn post-list
  "Date + title rows, as used by the archive, tag and author pages."
  [posts date-pattern]
  [:ul.archive-list
   (for [{:keys [uri title] :as post} posts]
     [:li (date (:date post) date-pattern) [:a {:href uri} title]])])
