(ns blobing.site
  "Client behaviour for the blog: theme toggle, mobile menu, and on-demand
  loading of syntax highlighting (highlight.js), math (KaTeX) and comments
  (self-hosted Remark42). Nothing third-party is fetched unless the page
  needs it.

  The initial theme is applied by the inline script in the document <head>
  (blobing.views.layout), before first paint; this namespace only reacts to
  the reader afterwards.

  Bundle size is a feature here (:advanced output is a few KB), and three
  measured rules keep it that way:
  - DOM collections are walked with `.forEach`, JS data is built with #js
    literals: `doseq` over `array-seq`, or `clj->js`, pulls ~95 KB of
    cljs.core into the bundle.
  - `str` on a value of unknown type does the same (its printer fallback),
    so non-literal string arguments carry a ^string hint.
  - Strings are compared with `identical?` (exact for JS strings): generic
    `=` dispatches through IEquiv and keeps every collection's equality code.
  - Third-party globals and dataset keys are read with goog.object by string
    name so :advanced renaming cannot break them."
  (:require [goog.object :as gobj]))

(def ^:private hljs-base "https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.9.0/")
(def ^:private hljs-languages #js ["clojure" "haskell" "latex" "lisp" "julia"])
(def ^:private katex-base "https://cdn.jsdelivr.net/npm/katex@0.16.11/dist/")
(defn- $ [selector] (.querySelector js/document selector))

(defn- each
  "Calls `f` on every element matching `selector`."
  [selector f]
  (.forEach (.querySelectorAll js/document selector) f))

;; ---------------------------------------------------------------------------
;; Loading

(defn- load-script
  "Appends a <script> and resolves once it has run."
  [src]
  (js/Promise.
   (fn [resolve reject]
     (let [el (.createElement js/document "script")]
       (set! (.-src el) src)
       (set! (.-async el) false)
       (set! (.-onload el) resolve)
       (set! (.-onerror el) reject)
       (.appendChild (.-head js/document) el)))))

(defn- load-style [href]
  (let [el (.createElement js/document "link")]
    (set! (.-rel el) "stylesheet")
    (set! (.-href el) href)
    (.appendChild (.-head js/document) el)))

;; ---------------------------------------------------------------------------
;; Theme

(def ^:private root (.-documentElement js/document))

(defn- stored-theme
  "The reader's saved theme, or nil. Blocked storage (private mode) throws;
  that is the same fact as 'no saved choice', so it reads as nil and the OS
  preference applies."
  []
  (try (.getItem js/localStorage "theme") (catch :default _ nil)))

(defn- remember-theme!
  "Persists the reader's choice. If storage is blocked the theme still
  applies to this page; it just is not remembered."
  [theme]
  (try (.setItem js/localStorage "theme" theme)
       (catch :default e (js/console.warn "theme choice not saved" e))))

(defn- current-theme []
  (if (identical? "dark" (.getAttribute root "data-theme")) "dark" "light"))

(defn- sync-comments!
  "Keeps an already-mounted Remark42 widget on the site's theme."
  [theme]
  (when-let [remark (gobj/get js/window "REMARK42")]
    (.call (gobj/get remark "changeTheme") remark theme)))

(defn- apply-theme! [theme persist?]
  (.setAttribute root "data-theme" theme)
  (when persist? (remember-theme! theme))
  (let [label (str "Switch to " (if (identical? theme "dark") "light" "dark") " theme")]
    (each ".theme-toggle"
          (fn [button]
            (.setAttribute button "aria-label" label)
            (.setAttribute button "title" label))))
  (sync-comments! theme))

(defn- toggle-theme! []
  (apply-theme! (if (identical? "dark" (current-theme)) "light" "dark") true))

(defn- follow-os-theme!
  "Follows the OS while the reader has not picked a theme themselves."
  [event]
  (when-not (stored-theme)
    (apply-theme! (if (.-matches event) "dark" "light") false)))

(defn- init-theme! []
  (each ".theme-toggle" #(.addEventListener % "click" toggle-theme!))
  (when (.-matchMedia js/window)
    (-> (.matchMedia js/window "(prefers-color-scheme: dark)")
        (.addEventListener "change" follow-os-theme!)))
  (apply-theme! (current-theme) false))

;; ---------------------------------------------------------------------------
;; Mobile menu

(defn- init-menu! []
  (let [sidebar ($ ".sidebar")
        button  ($ ".menu-toggle")
        classes (some-> sidebar .-classList)
        set-open! (fn [open?]
                    (.toggle classes "menu-open" open?)
                    (.setAttribute button "aria-expanded" (if open? "true" "false")))]
    (when (and sidebar button)
      (.addEventListener button "click"
                         #(set-open! (not (.contains classes "menu-open"))))
      (.addEventListener js/document "keydown"
                         (fn [event]
                           (when (and (identical? "Escape" (.-key event))
                                      (.contains classes "menu-open"))
                             (set-open! false)
                             (.focus button)))))))

;; ---------------------------------------------------------------------------
;; Syntax highlighting

(defn- load-hljs-language [^string lang]
  (load-script (str hljs-base "languages/" lang ".min.js")))

(defn- highlight-all! []
  (let [hljs (gobj/get js/window "hljs")]
    (.call (gobj/get hljs "configure") hljs #js {"ignoreUnescapedHTML" true})
    (each "pre code" #(.call (gobj/get hljs "highlightElement") hljs %))))

(defn- init-highlighting! []
  (when ($ "pre code")
    (-> (load-script (str hljs-base "highlight.min.js"))
        (.then #(.all js/Promise (.map hljs-languages load-hljs-language)))
        (.then highlight-all!)
        (.catch #(js/console.warn "highlight.js failed to load" %)))))

;; ---------------------------------------------------------------------------
;; Math

(def ^:private tex-pattern (js/RegExp. "\\$\\$|\\\\\\(|\\\\\\[|\\\\begin\\{"))

(defn- env-delimiter
  "Display delimiter for a LaTeX environment; KaTeX renders it whole."
  [^string env]
  #js {"left" (str "\\begin{" env "}") "right" (str "\\end{" env "}") "display" true})

(defn- math-options []
  #js {"delimiters"   (.concat #js [#js {"left" "$$" "right" "$$" "display" true}
                                    #js {"left" "\\[" "right" "\\]" "display" true}
                                    #js {"left" "\\(" "right" "\\)" "display" false}]
                               (.map #js ["equation" "equation*" "align" "align*" "gather" "gather*"]
                                     env-delimiter))
       "ignoredTags"  #js ["script" "noscript" "style" "textarea" "pre" "code"]
       "throwOnError" false})

(defn- init-math! []
  (when-let [article ($ ".prose")]
    (when (.test tex-pattern (.-textContent article))
      (load-style (str katex-base "katex.min.css"))
      (-> (load-script (str katex-base "katex.min.js"))
          (.then #(load-script (str katex-base "contrib/auto-render.min.js")))
          (.then #(.call (gobj/get js/window "renderMathInElement") js/window article (math-options)))
          (.catch #(js/console.warn "KaTeX failed to load" %))))))

;; ---------------------------------------------------------------------------
;; Section index: scroll-spy over the outline rendered by
;; blobing.views.components/outline-nav.

(def ^:private wide-screen "(min-width: 80rem)")

(def ^:private reading-line
  "Pixels from the top of the viewport: a heading above it has been reached."
  120)

(defn- link-target [link]
  (.slice (.getAttribute link "href") 1))

(defn- current-section
  "Id of the last outlined heading scrolled past the reading line, or nil."
  [links]
  (.reduce links
           (fn [current link]
             (let [heading (.getElementById js/document (link-target link))]
               (if (and heading (< (.-top (.getBoundingClientRect heading)) reading-line))
                 (link-target link)
                 current)))
           nil))

(defn- mark-current!
  "Flags the link for section `id`; keeps it in view when the index is a rail."
  [links id rail?]
  (.forEach links
            (fn [link]
              (if (identical? (link-target link) id)
                (do (.setAttribute link "aria-current" "true")
                    (when rail? (.scrollIntoView link #js {"block" "nearest"})))
                (.removeAttribute link "aria-current")))))

(defn- sync-open!
  "The index is always open as a rail, collapsed above the text on narrow screens."
  [details wide?]
  (if wide?
    (.setAttribute details "open" "")
    (.removeAttribute details "open")))

(defn- init-outline! []
  (when-let [outline ($ ".outline")]
    (let [details (.querySelector outline "details")
          links   (js/Array.from (.querySelectorAll outline "a"))
          wide    (.matchMedia js/window wide-screen)
          state   #js {"current" nil "queued" false}
          refresh (fn []
                    (gobj/set state "queued" false)
                    (let [id (current-section links)]
                      (when-not (identical? id (gobj/get state "current"))
                        (gobj/set state "current" id)
                        (mark-current! links id (.-matches wide)))))]
      (sync-open! details (.-matches wide))
      (.addEventListener wide "change" #(sync-open! details (.-matches %)))
      (.addEventListener js/window "scroll"
                         (fn []
                           (when-not (gobj/get state "queued")
                             (gobj/set state "queued" true)
                             (js/requestAnimationFrame refresh)))
                         #js {"passive" true})
      (refresh))))

;; ---------------------------------------------------------------------------
;; Comments: self-hosted Remark42 (threads, replies, social and Keycloak
;; logins; markdown sanitized server-side). The mount carries its config as
;; data-* attributes rendered by blobing.views.components/comments.

(defn- remark-config
  "Remark42's embed reads `window.remark_config`; dataset and global keys go
  through goog.object so :advanced renaming cannot touch them."
  [data]
  #js {"host"       (gobj/get data "host")
       "site_id"    (gobj/get data "siteId")
       "url"        (gobj/get data "url")
       "locale"     (gobj/get data "locale")
       "theme"      (current-theme)
       "components" #js ["embed"]
       "no_footer"  true})

(defn- init-comments! []
  (when-let [mount ($ "#remark42[data-host]")]
    (let [config (remark-config (.-dataset mount))
          ^string host (gobj/get config "host")]
      (gobj/set js/window "remark_config" config)
      (-> (load-script (str host "/web/embed.js"))
          (.catch #(js/console.warn "Remark42 failed to load" %))))))

;; ---------------------------------------------------------------------------

(defn init! []
  (init-theme!)
  (init-menu!)
  (init-outline!)
  (init-highlighting!)
  (init-math!)
  (init-comments!))

(if (identical? "loading" (.-readyState js/document))
  (.addEventListener js/document "DOMContentLoaded" init!)
  (init!))
