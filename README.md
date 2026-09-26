# Visit me at

CC - Attribute me - Share Alike; You can monetize the way you want, though.
[![img](https://img.shields.io/badge/License-CC%20BY--SA%204.0-lightgrey.svg)](http://creativecommons.org/licenses/by-sa/4.0/) 🅭🅯🄎


**The *working* website**:
[![Netlify Status](https://api.netlify.com/api/v1/badges/b64c13cc-8fc6-4bea-8510-c2670a7a0d0b/deploy-status)](https://app.netlify.com/sites/buddhilw/deploys)

[buddhilw.com](www.buddhilw.com)

## Development

A [Cryogen](http://cryogenweb.org) site. Clojure renders every page with
hiccup, ClojureScript adds the client behaviour, and there is no Sass or npm
step.

| Task | Command |
|---|---|
| Build `public/` | `lein run` (or `clojure -M:build`) |
| Serve with live rebuild on :3003 | `clojure -X:serve :port 3003` |
| Unit tests | `lein test` (or `clojure -M:test`) |
| Compile the client bundle | `clojure -M:cljs` → `themes/lotus/js/site.js` |

`themes/lotus/js/site.js` is committed so a site build never needs the
ClojureScript compiler. Recompile it after changing `src/cljs`.

### Layout

```
src/clj/cryogen/content.clj    normalizes every post's DOM (headings, TOCs, descriptions)
src/clj/blobing/views/*.clj    hiccup views: layout, components, one `view` method per page
src/clj/blobing/render.clj     `{% page <view> %}` Selmer tag that hands cryogen's context to the views
src/cljs/blobing/site.cljs     theme toggle, mobile menu, on-demand highlight.js / KaTeX / comments
themes/lotus/css/site.css      the one stylesheet: tokens, light + dark themes, layout, prose
themes/lotus/html/*.html       one-line bridges ({% page post %}); portfolio pages keep their Selmer templates
```

### Writing posts: headings

The post title is the page's only `<h1>`. Write sections with whatever
markdown levels read naturally (`#`, `##`, ...): at build time
`cryogen.content` re-ranks them so the shallowest level used becomes `<h2>`
and levels never skip. It also:

- builds the page's "On this page" index from the h2/h3 headings (a sticky
  rail with scroll-spy on wide screens, a collapsed block on phones), so a
  hand-made table of contents (org-export `# Table of Contents`, or
  markdown-toc's `**Table of Contents**` + list) is dropped as redundant;
- gives every heading a unique id and removes hand-written anchors that
  duplicate one;
- drops a first heading that just repeats the title;
- derives the meta description from the first real paragraphs (a
  `:description` in the post metadata always wins).

Math is rendered by KaTeX, loaded only on pages that contain TeX. In markdown
write inline math as `\\( ... \\)` and display math as `$$ ... $$` or
`\begin{equation}`. flexmark reads `{...}` as attributes, so escape braces
inside math: `\mathbf\{a\}`.

### Comments

Comments are self-hosted [Remark42](https://github.com/umputun/remark42)
(threads, replies, social logins, server-side sanitization). Set its URL in
`content/config.edn`:

```clojure
:comments {:host "https://comments.example.com" :site-id "blobing" :locale "en"}
```

With a blank `:host` no comments section is rendered.

### Fonts

`themes/lotus/fonts/*.woff2` are subsets of the upstream TTFs (Montserrat,
Oxygen, Inconsolata; OFL). Regenerate them with fonttools:

```sh
fonttools varLib.instancer Montserrat-VariableFont_wght.ttf wght=400:800 -o m.ttf
pyftsubset m.ttf --flavor=woff2 --output-file=montserrat-var.woff2 \
  --unicodes="U+0000-00FF,U+0100-017F,U+2000-206F,U+20AC,U+2122,U+2190-2199,U+2212"
```
