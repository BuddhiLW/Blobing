(ns blobing.views.pages
  "One `view` method per page type. Each takes cryogen's template context
  (the params map selmer would have seen) and returns a full HTML document.

  The article title is the page's only <h1>; body headings were re-ranked to
  start at <h2> by cryogen.content."
  (:require [blobing.views.components :as c]
            [blobing.views.layout :as layout]))

(defmulti view
  "Renders the page `kind` (:post, :page, :home, ...) for cryogen context `ctx`."
  (fn [kind _ctx] kind))

(defmethod view :default [kind _ctx]
  (throw (ex-info (str "No view for " kind) {:kind kind :known (keys (methods view))})))

(defn- article-body
  "Header, decorative band, section index and prose shared by posts and
  pages. They are siblings so the article can lay the index out as a rail."
  [{:keys [title date author tags outline content]} {:keys [band?]}]
  (list
   [:header.post-header
    [:h1.post-title title]
    (when date
      [:p.post-meta (c/date date) (when author (list " · " author))])
    (c/tag-list tags)]
   (when band?
     [:div.fancy-area {:aria-hidden "true"} [:div.fancy]])
   (c/outline-nav outline)
   [:div.prose (c/raw content)]))

(defmethod view :post [_ {:keys [post site-url uri] :as ctx}]
  (layout/document
   ctx
   {:subtitle (:title post)
    :article  post
    :body     [:article.post
               (article-body post {:band? true})
               (c/prev-next post)
               (c/comments (:comments ctx) (str site-url uri))]}))

(defmethod view :page [_ {:keys [page] :as ctx}]
  (layout/document
   ctx
   {:subtitle (:title page)
    :article  page
    :body     [:article.page
               (article-body (dissoc page :date :author) {:band? false})
               (c/prev-next page)]}))

(defmethod view :home [_ {:keys [title description latest-posts archives-uri] :as ctx}]
  (layout/document
   ctx
   {:body (list
           [:section.intro
            [:h1.page-title title]
            [:p.lede description]]
           [:section {:aria-labelledby "recent-title"}
            [:h2#recent-title.section-title "Recent posts"]
            [:ol.cards
             (for [{:keys [uri date] :as post} latest-posts]
               [:li.card
                [:h3 [:a {:href uri} (:title post)]]
                [:p.post-meta (c/date date)]
                (when-let [excerpt (:description post)]
                  [:p.card-excerpt excerpt])])]
            [:p [:a.button {:href archives-uri} "All posts →"]]])}))

(defn- grouped-archive [ctx heading groups]
  (layout/document
   ctx
   {:subtitle heading
    :body     (list
               [:header.post-header [:h1.page-title heading]]
               (for [{:keys [group posts]} groups]
                 [:section.archive-group
                  [:h2 group]
                  (c/post-list posts "MMM dd")]))}))

(defmethod view :archives [_ {:keys [groups] :as ctx}]
  (grouped-archive ctx "Posts" groups))

(defmethod view :author [_ {:keys [author groups] :as ctx}]
  (grouped-archive ctx (str "Posts by " author) groups))

(defmethod view :tags [_ {:keys [tags] :as ctx}]
  (layout/document
   ctx
   {:subtitle "Tags"
    :body     (list
               [:header.post-header [:h1.page-title "Tags"]]
               [:ul.tag-cloud
                (for [{:keys [uri name]} (sort-by :name tags)]
                  [:li [:a.tag {:href uri} name]])])}))

(defmethod view :tag [_ {:keys [name posts] :as ctx}]
  (let [heading (str "Tagged “" name "”")]
    (layout/document
     ctx
     {:subtitle heading
      :body     (list
                 [:header.post-header [:h1.page-title heading]]
                 (c/post-list posts "MMM yyyy"))})))

(defmethod view :previews [_ {:keys [posts prev-uri next-uri] :as ctx}]
  (layout/document
   ctx
   {:body (list
           (for [{:keys [uri content date] :as post} posts]
             [:article.card
              [:h2 [:a {:href uri} (:title post)]]
              [:p.post-meta (c/date date)]
              [:div.prose (c/raw content)]
              [:p [:a.button {:href uri} "Continue reading →"]]])
           (when (or prev-uri next-uri)
             [:nav.prev-next {:aria-label "More posts"}
              (when prev-uri [:a.prev {:href prev-uri} [:span.label "← Newer"]])
              (when next-uri [:a.next {:href next-uri} [:span.label "Older →"]])]))}))
