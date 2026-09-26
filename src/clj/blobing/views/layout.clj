(ns blobing.views.layout
  "The document shell every view renders into: <head>, sidebar, footer."
  (:require [hiccup2.core :as h]
            [clojure.java.io :as io]
            [net.cgrand.enlive-html :as enlive]
            [clojure.string :as str]))

(def ^:private theme-bootstrap
  ;; Runs in <head> before first paint so the page never flashes the wrong
  ;; theme: saved choice, else the OS preference. Kept inline on purpose;
  ;; everything else lives in the compiled blobing.site bundle. The empty
  ;; catch is deliberate: localStorage throws when storage is blocked
  ;; (private mode), and the OS preference is then the right answer.
  (h/raw
   (str "(function(){var t;try{t=localStorage.getItem('theme')}catch(e){/* storage blocked: use OS preference */}"
        "if(t!=='light'&&t!=='dark'){t=window.matchMedia&&"
        "window.matchMedia('(prefers-color-scheme: dark)').matches?'dark':'light'}"
        "var d=document.documentElement;d.setAttribute('data-theme',t);d.classList.add('js')})();")))

(defn- asset [{:keys [blog-prefix]} path]
  (str blog-prefix path))

(def ^:private sprite-file "themes/lotus/img/icons.svg")

(def ^:private sprite
  "id -> {:view-box .. :paths [..]} for every <symbol> in the theme's icon
  sprite, read once. Icons are inlined so pages make no sprite request and
  never render an empty button while <use> resolves."
  (delay
    (into {}
          (for [sym (enlive/select (enlive/html-resource (io/file sprite-file)) [:symbol])]
            [(get-in sym [:attrs :id])
             {:view-box (get-in sym [:attrs :viewbox] (get-in sym [:attrs :viewBox]))
              :paths    (map #(get-in % [:attrs :d]) (enlive/select sym [:path]))}]))))

(defn- icon
  "Inline SVG for sprite symbol `id` (e.g. \"icon-rss\")."
  [id]
  (let [{:keys [view-box paths]} (get @sprite id)]
    (into [:svg {:viewBox view-box :aria-hidden "true" :class id}]
          (for [d paths] [:path {:d d}]))))

(def ^:private moon
  [:svg.icon-moon {:viewBox "0 0 24 24" :aria-hidden "true"}
   [:path {:d "M21 12.8A9 9 0 1 1 11.2 3a7 7 0 0 0 9.8 9.8z"}]])

(def ^:private sun
  [:svg.icon-sun {:viewBox "0 0 24 24" :aria-hidden "true"}
   [:circle {:cx "12" :cy "12" :r "4.5"}]
   [:path {:d "M12 1.5v2.5M12 20v2.5M4.6 4.6l1.8 1.8M17.6 17.6l1.8 1.8M1.5 12H4M20 12h2.5M4.6 19.4l1.8-1.8M17.6 6.4l1.8-1.8"}]])

(defn- nav-link [href label current?]
  [:li [:a (cond-> {:href href} current? (assoc :aria-current "page")) label]])

(defn- sidebar [{:keys [title index-uri archives-uri tags-uri rss-uri navbar-pages
                        home archives uri page] :as ctx}]
  [:header.sidebar
   [:div.sidebar-top
    [:a.brand {:href index-uri :title title}
     [:img.avatar {:src (asset ctx "/img/avatar-192.webp") :alt "" :width 96 :height 96}]
     [:span.brand-title title]]
    [:div.sidebar-controls
     [:button.icon-button.theme-toggle {:type "button" :aria-label "Toggle theme"} moon sun]
     [:button.icon-button.menu-toggle {:type          "button"
                                       :aria-expanded "false"
                                       :aria-controls "site-nav"
                                       :aria-label    "Menu"}
      (icon "icon-menu")]]]
   [:nav#site-nav.site-nav {:aria-label "Main"}
    [:ul
     (nav-link index-uri "Home" home)
     (nav-link archives-uri "Posts" archives)
     (for [nav-page navbar-pages]
       (nav-link (:uri nav-page) (:title nav-page) (= (:uri page) (:uri nav-page))))
     (nav-link tags-uri "Tags" (= uri tags-uri))
     (nav-link rss-uri "RSS" false)]]
   [:ul.social
    [:li [:a {:href "https://github.com/BuddhiLW" :rel "external me" :title "GitHub" :aria-label "GitHub"}
          (icon "icon-github-circled")]]
    [:li [:a {:href "https://www.linkedin.com/in/pedro-g-branquinho/" :rel "external me" :title "LinkedIn" :aria-label "LinkedIn"}
          (icon "icon-linkedin")]]
    [:li [:a {:href rss-uri :title "RSS feed" :aria-label "RSS feed"}
          (icon "icon-rss")]]]])

(defn- footer [{:keys [author today]}]
  [:footer.site-footer
   [:p "© " (.format (java.text.SimpleDateFormat. "yyyy") today) " " author
    " · Content licensed "
    [:a {:href "https://creativecommons.org/licenses/by-sa/4.0/" :rel "license"} "CC BY-SA 4.0"]]
   [:p "Built with " [:a {:href "http://cryogenweb.org"} "Cryogen"]
    " · Theme based on Lotus by " [:a {:href "http://github.com/KingMob"} "KingMob"]
    " · " [:a {:href "http://github.com/BuddhiLW/Blobing"} "Source"]]])

(defn- meta-tags
  "description + OpenGraph tags. `article` is the post or page being shown,
  nil for listings."
  [{:keys [site-url uri description]} article]
  (let [desc (or (:description article) description)]
    (list
     (when desc [:meta {:name "description" :content desc}])
     (when (seq (:tags article))
       [:meta {:name "keywords" :content (str/join "," (map :name (:tags article)))}])
     (when article
       (list
        (when desc [:meta {:property "og:description" :content desc}])
        [:meta {:property "og:url" :content (str site-url uri)}]
        [:meta {:property "og:title" :content (:title article)}]
        [:meta {:property "og:type" :content "article"}])))))

(defn document
  "Full HTML document around `body`. `subtitle` is appended to the site title;
  `article` feeds the meta/OpenGraph tags."
  [{:keys [title rss-uri] :as ctx} {:keys [subtitle article body]}]
  (str
   "<!DOCTYPE html>\n"
   (h/html
    {:mode :html}
    [:html {:lang "en" :data-theme "light"}
     [:head
      [:meta {:charset "utf-8"}]
      [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
      [:title (str title (when subtitle (str ": " subtitle)))]
      (meta-tags ctx article)
      [:meta {:name "color-scheme" :content "light dark"}]
      [:meta {:name "theme-color" :content "#522158"}]
      [:script theme-bootstrap]
      [:link {:rel "alternate" :type "application/rss+xml" :title title :href rss-uri}]
      [:link {:rel "icon" :type "image/png" :href (asset ctx "/img/favicon-32.png")}]
      [:link {:rel "apple-touch-icon" :href (asset ctx "/img/apple-touch-icon.png")}]
      [:link {:rel "preload" :as "font" :type "font/woff2" :crossorigin "anonymous"
              :href (asset ctx "/fonts/oxygen-400.woff2")}]
      (list [:link {:rel "stylesheet", :href (asset ctx "/css/normalize.css")}] [:link {:rel "stylesheet", :href (asset ctx "/css/site.css")}])]
     [:body
      [:a.skip-link {:href "#main"} "Skip to content"]
      [:div.shell
       (sidebar ctx)
       [:main#main.main
        [:div.content body]
        (footer ctx)]]
      [:script {:src (asset ctx "/js/site.js") :defer true}]
      (when-let [klipse (or (get-in ctx [:post :klipse]) (get-in ctx [:page :klipse]))]
        (h/raw klipse))]])))
