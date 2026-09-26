(ns cryogen.content-test
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.string :as str]
            [net.cgrand.enlive-html :as enlive]
            [cryogen.content :as content]
            [cryogen-core.compiler]))

(defn- dom [html]
  (enlive/html-snippet html))

(defn- tags [nodes]
  (->> (enlive/select nodes [#{:h1 :h2 :h3 :h4 :h5 :h6}])
       (map :tag)))

(defn- render [nodes]
  (apply str (enlive/emit* nodes)))

(deftest heading-rank-map-test
  (testing "shallowest level used becomes h2, gaps close"
    (is (= {1 2 2 3 3 4} (content/heading-rank-map #{1 2 3})))
    (is (= {2 2 3 3} (content/heading-rank-map #{2 3})))
    (is (= {1 2 3 3} (content/heading-rank-map #{1 3})))
    (is (= {3 2} (content/heading-rank-map #{3}))))
  (testing "never exceeds h6"
    (is (= 6 (get (content/heading-rank-map #{1 2 3 4 5 6}) 6)))))

(deftest normalize-headings-test
  (testing "h1 sections under the title are demoted"
    (is (= [:h2 :h3 :h2]
           (tags (content/normalize-dom (dom "<h1>A</h1><h2>B</h2><h1>C</h1>") "Title")))))
  (testing "a post that starts at ### still starts at h2"
    (is (= [:h2 :h3]
           (tags (content/normalize-dom (dom "<h3>A</h3><h4>B</h4>") "Title")))))
  (testing "nested headings are ranked too"
    (is (= [:h2 :h3]
           (tags (content/normalize-dom (dom "<h1>A</h1><blockquote><h3>B</h3></blockquote>") "T"))))))

(deftest hand-toc-test
  (testing "org-export TOC heading + list is dropped (the page renders its own index)"
    (let [out (content/normalize-dom
               (dom "<h1 id=\"table-of-contents\">Table of Contents</h1>\n<ol><li><a href=\"#x\">X</a></li></ol><h1 id=\"x\">X</h1>")
               "T")]
      (is (empty? (enlive/select out [:ol])))
      (is (= [:h2] (tags out)) "the TOC heading no longer counts as a section")))
  (testing "markdown-toc bold paragraph between comments is dropped"
    (let [out (content/normalize-dom
               (dom "<!-- markdown-toc start --><p><strong>Table of Contents</strong></p>\n<ul><li>a</li></ul><!-- markdown-toc end --><h1>a</h1>")
               "T")]
      (is (empty? (enlive/select out [:ul])))
      (is (not (str/includes? (render out) "Table of Contents")))))
  (testing "a marker with nothing to wrap is left alone"
    (let [out (content/normalize-dom (dom "<p><strong>Contents</strong></p><p>text</p>") "T")]
      (is (= ["Contents" "text"] (map enlive/text (enlive/select out [:p])))))))

(deftest ensure-heading-ids-test
  (let [out (content/normalize-dom
             (dom "<h1>Ação Rápida!</h1><h1 id=\"kept\">Kept</h1><h1>Ação rápida</h1><h1></h1>")
             "T")]
    (is (= ["acao-rapida" "kept" "acao-rapida-2" "section"]
           (map #(get-in % [:attrs :id]) (enlive/select out [:h2]))))))

(deftest redundant-anchor-test
  (let [out (content/normalize-dom
             (dom "<a id=\"the-problem\"></a><h2 id=\"the-problem\">The Problem</h2><a id=\"org1\"></a><h2>Other</h2>")
             "T")]
    (is (= 1 (count (enlive/select out [(enlive/attr= :id "the-problem")])))
        "the anchor duplicating a heading id is gone")
    (is (= 1 (count (enlive/select out [[:a (enlive/attr= :id "org1")]])))
        "an anchor other links rely on is kept")))

(deftest outline-test
  (let [out (content/normalize-dom
             (dom "<h1 id=\"a\">Alpha</h1><h2 id=\"b\">Beta</h2><h3 id=\"c\">Gamma</h3><h1 id=\"d\">Delta</h1>")
             "T")]
    (testing "h2/h3 in document order; the # self-link is not part of the text"
      (is (= [{:id "a" :level 2 :text "Alpha"}
              {:id "b" :level 3 :text "Beta"}
              {:id "d" :level 2 :text "Delta"}]
             (content/outline out))))
    (testing "normalize-article attaches it"
      (is (= "a" (-> (content/normalize-article {:title "T" :content-dom (dom "<h1 id=\"a\">Alpha</h1>")}
                                                {:blocks-per-preview 2})
                     :outline first :id))))))

(deftest org-anchor-test
  (testing "empty anchor paragraphs are unwrapped, anchor kept"
    (let [out (content/normalize-dom (dom "<p><a id=\"org123\"></a></p><h1>A</h1>") "T")]
      (is (empty? (enlive/select out [:p])))
      (is (= 1 (count (enlive/select out [(enlive/attr= :id "org123")])))))))

(deftest title-heading-test
  (is (= [:h2]
         (tags (content/normalize-dom (dom "<h1>My Post</h1><h2>Body</h2>") "my post")))
      "a leading heading repeating the title is dropped"))

(deftest decorate-test
  (let [out (content/normalize-dom
             (dom "<h1 id=\"a\">A</h1><table><tr><td>1</td></tr></table><img src=\"x.png\">")
             "T")]
    (is (= "#a" (-> (enlive/select out [:h2 :a.heading-anchor]) first :attrs :href)))
    (is (= "A" (-> (enlive/select out [:h2]) first :content first)) "anchor goes after the text")
    (is (= 1 (count (enlive/select out [:div.table-wrap :table]))))
    (is (= "lazy" (-> (enlive/select out [:img]) first :attrs :loading)))))

(deftest summarize-test
  (is (= "First para. Second para."
         (content/summarize
          (content/normalize-dom
           (dom "<h1>Table of Contents</h1><ol><li>x</li></ol><h1>A</h1><p>First\n para.</p><pre>code</pre><p>Second para.</p><p>Third.</p>")
           "T"))))
  (is (nil? (content/summarize (dom "<h1>Only a heading</h1>"))))
  (is (<= (count (content/summarize (dom (str "<p>" (apply str (repeat 100 "word ")) "</p>")))) 201)))

(deftest summarize-skips-display-math-test
  (is (= "Prose after the formula."
         (content/summarize
          (dom "<p>\\begin{equation} f(x) = 1 \\end{equation}</p><p>$$x^2$$</p><p>Prose after the formula.</p>")))))

(deftest normalize-article-description-test
  (let [config  {:blocks-per-preview 2}
        article {:title "T" :content-dom (dom "<h1>Table of Contents</h1><ol><li>x</li></ol><p>Real text.</p>")}]
    (testing "cryogen's auto description is replaced"
      (let [auto (:description (cryogen-core.compiler/add-description config article))]
        (is (= "Real text."
               (:description (content/normalize-article (assoc article :description auto) config))))))
    (testing "a description written in the metadata is kept"
      (is (= "Hand written"
             (:description (content/normalize-article (assoc article :description "Hand written") config)))))))
