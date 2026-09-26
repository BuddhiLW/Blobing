(ns blobing.views.pages-test
  (:require [blobing.views.pages :as pages]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [net.cgrand.enlive-html :as enlive]))

(def ^:private date (java.util.Date. 126 0 20)) ; 2026-01-20

(def ^:private ctx
  "Minimal cryogen template context."
  {:title        "Dark Cyber Corner"
   :description  "My little dark corner"
   :author       "BuddhiLW"
   :blog-prefix  ""
   :site-url     "https://buddhilw.com"
   :today        date
   :index-uri    "/"
   :archives-uri "/archives/"
   :tags-uri     "/tags/"
   :rss-uri      "/feed.xml"
   :navbar-pages [{:uri "/pages-output/evil/" :title "Evil Deeds"}]
   :comments     {:host "" :site-id "blobing"}})

(def ^:private post
  {:title       "Tom & <Jerry>"
   :date        date
   :description "About cats"
   :tags        [{:name "clojure" :uri "/tags-output/clojure/"}]
   :content     "<h2 id=\"a\">Section</h2><p>Body</p>"
   :prev        {:uri "/posts-output/newer/" :title "Newer"}})

(defn- render [kind extra]
  (enlive/html-snippet (pages/view kind (merge ctx extra))))

(defn- texts [nodes selector]
  (map enlive/text (enlive/select nodes selector)))

(deftest post-view-test
  (let [page (render :post {:post post :uri "/posts-output/x/"})]
    (testing "the title is the only h1, and it is escaped"
      (is (= ["Tom & <Jerry>"] (texts page [:h1])))
      (is (= 1 (count (enlive/select page [:h1])))))
    (testing "article HTML is passed through, not escaped"
      (is (= ["Section"] (texts page [:.prose :h2]))))
    (testing "tags, date and prev link render"
      (is (= ["clojure"] (texts page [:.tag-list :a])))
      (is (= "2026-01-20" (-> (enlive/select page [:.post-meta :time]) first :attrs :datetime)))
      (is (= ["← NewerNewer"] (texts page [:.prev-next :a.prev]))))
    (testing "meta description comes from the post"
      (is (= "About cats"
             (-> (enlive/select page [[:meta (enlive/attr= :name "description")]]) first :attrs :content))))))

(deftest comments-test
  (testing "no comments section while no Remark42 host is configured"
    (is (empty? (enlive/select (render :post {:post post}) [:#comments]))))
  (testing "configured host mounts Remark42 with the page URL"
    (let [page  (render :post {:post post :uri "/posts-output/x/"
                               :comments {:host "https://comments.example" :site-id "blobing"}})
          mount (first (enlive/select page [:#remark42]))]
      (is (= "https://comments.example" (get-in mount [:attrs :data-host])))
      (is (= "https://buddhilw.com/posts-output/x/" (get-in mount [:attrs :data-url]))))))

(deftest navigation-test
  (testing "the current page is marked for assistive tech"
    (let [page (render :archives {:archives true :groups []})]
      (is (= ["Posts"] (texts page [[:a (enlive/attr= :aria-current "page")]])))))
  (testing "navbar pages from content are listed"
    (is (some #{"Evil Deeds"} (texts (render :tags {:tags []}) [:.site-nav :a])))))

(deftest home-test
  (let [page (render :home {:home true
                            :latest-posts [{:uri "/p/1/" :title "One" :date date :description "first"}
                                           {:uri "/p/2/" :title "Two" :date date}]})]
    (is (= ["One" "Two"] (texts page [:.card :h3])))
    (is (= ["first"] (texts page [:.card-excerpt])))))

(deftest document-shell-test
  (let [html (pages/view :tags (assoc ctx :tags []))]
    (is (str/starts-with? html "<!DOCTYPE html>"))
    (is (str/includes? html "localStorage.getItem('theme')") "theme bootstrap script is not escaped")
    (is (str/includes? html "/js/site.js"))))

(deftest unknown-view-test
  (is (thrown-with-msg? clojure.lang.ExceptionInfo #"No view for :nope"
                        (pages/view :nope ctx))))
