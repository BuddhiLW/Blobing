(ns cryogen.content
  "Normalizes the rendered DOM of posts and pages so the theme can style every
  article the same way, whatever tool wrote its markdown (hand-written,
  org-export, markdown-toc).

  The article title is always rendered by the template as the only <h1>, so
  body headings are re-ranked to start at <h2> and never skip a level.

  Everything here is a pure function over enlive nodes; `normalize-article` is
  the `:update-article-fn` hook handed to cryogen."
  (:require [clojure.string :as str]
            [clojure.walk :as walk]
            [cryogen-core.compiler :as compiler]))

;; ---------------------------------------------------------------------------
;; Node predicates

(def heading-levels {:h1 1 :h2 2 :h3 3 :h4 4 :h5 5 :h6 6})

(defn heading-level [node]
  (when (map? node) (heading-levels (:tag node))))

(defn- level->tag [level]
  (keyword (str "h" level)))

(defn text-of
  "Concatenated text content of an enlive node (or seq of nodes)."
  [node]
  (cond
    (string? node) node
    (map? node)    (if (= :comment (:type node)) "" (text-of (:content node)))
    (seqable? node) (apply str (map text-of node))
    :else ""))

(defn- blank-node?
  "Whitespace text and HTML comments: things that sit between blocks without
  being content."
  [node]
  (or (and (string? node) (str/blank? node))
      (and (map? node) (= :comment (:type node)))))

(def ^:private toc-title
  #"(?i)\s*(table of contents|contents|índice|sumário)\s*:?\s*")

(defn- toc-marker?
  "A heading (org-export) or a bold paragraph (markdown-toc) announcing a
  table of contents."
  [node]
  (and (map? node)
       (or (heading-level node) (= :p (:tag node)))
       (re-matches toc-title (text-of node))))

(defn- list-node? [node]
  (and (map? node) (#{:ul :ol} (:tag node))))

;; ---------------------------------------------------------------------------
;; Passes over the top-level block sequence

(defn- toc-nav [list-node]
  {:tag     :nav
   :attrs   {:class "toc" :aria-label "Table of contents"}
   :content [{:tag     :details
              :attrs   {:open ""}
              :content [{:tag :summary :attrs nil :content ["Contents"]}
                        list-node]}]})

(defn extract-tocs
  "Replaces every hand-made table of contents (a TOC marker followed by a
  list) with a single `nav.toc` element. A marker with no list after it is
  left alone."
  [nodes]
  (loop [nodes (seq nodes) out []]
    (if-let [[node & more] nodes]
      (if (toc-marker? node)
        (let [[gap [lst & after]] (split-with blank-node? more)]
          (if (list-node? lst)
            (recur after (conj out (toc-nav lst)))
            (recur more (conj out node))))
        (recur more (conj out node)))
      out)))

(defn- empty-anchor-paragraph?
  "org-export emits `<a id=\"orgXXXX\"></a>` on its own line, which markdown
  wraps in an otherwise empty <p>, leaving a stray gap before each heading."
  [node]
  (and (map? node)
       (= :p (:tag node))
       (seq (:content node))
       (every? #(or (blank-node? %)
                    (and (map? %) (= :a (:tag %)) (str/blank? (text-of %))))
               (:content node))))

(defn unwrap-empty-anchors
  "Keeps org anchors (TOC links point at them) but drops their empty <p>."
  [nodes]
  (mapcat (fn [node]
            (if (empty-anchor-paragraph? node)
              (remove blank-node? (:content node))
              [node]))
          nodes))

(defn drop-title-heading
  "Removes a leading heading that merely repeats the article title."
  [nodes title]
  (let [[before [node & after]] (split-with (complement heading-level) nodes)
        same? (fn [a b] (= (str/lower-case (str/trim (str a)))
                           (str/lower-case (str/trim (str b)))))]
    (if (and node
             (every? blank-node? before)
             (same? (text-of node) title))
      (concat before after)
      nodes)))

;; ---------------------------------------------------------------------------
;; Heading hierarchy

(defn- collect-levels [nodes]
  (let [levels (atom #{})]
    (walk/postwalk (fn [n] (when-let [l (heading-level n)] (swap! levels conj l)) n)
                   nodes)
    @levels))

(defn heading-rank-map
  "Maps each heading level used in the body to its rank-based level: the
  shallowest becomes 2, the next 3, and so on (capped at 6). `#`/`###` in a
  post becomes h2/h3 — the gap closes and nothing competes with the title."
  [levels]
  (into {} (map-indexed (fn [i level] [level (min 6 (+ 2 i))]) (sort levels))))

(defn normalize-headings [nodes]
  (let [ranks (heading-rank-map (collect-levels nodes))]
    (walk/postwalk (fn [n]
                     (if-let [l (heading-level n)]
                       (assoc n :tag (level->tag (ranks l)))
                       n))
                   nodes)))

;; ---------------------------------------------------------------------------
;; Presentation affordances

(defn- anchor-link [id]
  {:tag     :a
   :attrs   {:class "heading-anchor" :href (str "#" id) :aria-hidden "true" :tabindex "-1"}
   :content ["#"]})

(defn decorate
  "Adds what the stylesheet relies on: self-links on headings, scrollable
  wrappers around tables, lazy-loaded images."
  [nodes]
  (walk/postwalk
   (fn [n]
     (cond
       (and (heading-level n) (get-in n [:attrs :id]))
       (update n :content #(conj (vec %) (anchor-link (get-in n [:attrs :id]))))

       (and (map? n) (= :table (:tag n)))
       {:tag :div :attrs {:class "table-wrap"} :content [n]}

       (and (map? n) (= :img (:tag n)))
       (update n :attrs #(merge {:loading "lazy"} %))

       :else n))
   nodes))

;; ---------------------------------------------------------------------------
;; Description (meta tags, home-page cards)

(defn- truncate-words [s n]
  (if (<= (count s) n)
    s
    (let [cut (subs s 0 n)
          end (or (str/last-index-of cut " ") n)]
      (str (str/trimr (subs cut 0 end)) "…"))))

(defn- math-only?
  "A paragraph that is a display-math block rather than prose."
  [text]
  (boolean (re-find #"^(\$\$|\\\[|\\begin\{)" text)))

(defn summarize
  "Plain-text summary from the first paragraphs of a normalized DOM: TOCs,
  headings, code and display math never leak into it."
  [nodes]
  (let [paragraphs (->> nodes
                        (filter #(and (map? %) (= :p (:tag %))))
                        (map (comp #(str/replace % #"\s+" " ") str/trim text-of))
                        (remove str/blank?)
                        (remove math-only?))]
    (some-> (seq (take 2 paragraphs))
            (->> (str/join " "))
            (truncate-words 200))))

(defn- auto-description?
  "True when `description` is the one cryogen derived itself, as opposed to
  one written in the post's metadata."
  [article config]
  (let [derived (:description (compiler/add-description
                               config (assoc article :description nil)))]
    (= (:description article) derived)))

;; ---------------------------------------------------------------------------
;; Cryogen hook

(defn normalize-dom [nodes title]
  (-> nodes
      unwrap-empty-anchors
      extract-tocs
      (drop-title-heading title)
      normalize-headings
      decorate
      vec))

(defn normalize-article
  "`:update-article-fn` for cryogen: runs on every parsed post and page."
  [article config]
  (let [auto? (auto-description? article config)
        article (update article :content-dom normalize-dom (:title article))]
    (cond-> article
      auto? (assoc :description (summarize (:content-dom article))))))

(def hooks
  "Overrides-and-hooks map passed to `compile-assets-timed`."
  {:update-article-fn normalize-article})
