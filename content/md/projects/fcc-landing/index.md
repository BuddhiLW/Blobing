{:title "Landing Page"
 :layout :page
 :page-index 1}

<!DOCTYPE html>
<!-- saved from url=(0012)about:srcdoc -->
<html lang="en">
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<link rel="apple-touch-icon" type="image/png" href="https://cpwebassets.codepen.io/assets/favicon/apple-touch-icon-5ae1a0698dcc2402e9712f7d01ed509a57814f994c660df9f7a952f3060705ee.png">
<meta name="apple-mobile-web-app-title" content="CodePen">
<link rel="shortcut icon" type="image/x-icon" href="https://cpwebassets.codepen.io/assets/favicon/favicon-aec34940fbc1a6e787974dcd360f2c6b63348d4b1f4e06c77743096d55480f33.ico">
<link rel="mask-icon" type="image/x-icon" href="https://cpwebassets.codepen.io/assets/favicon/logo-pin-8f3771b1072e3c38bd662872f6b673a722f4b3ca2421637d5596661b4e2132cc.svg" color="#111">

<title>CodePen - FCC: Product Landing Page</title>

<link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/font-awesome/4.6.3/css/font-awesome.min.css">

<link rel="stylesheet" href="./style.css">
<script>
window.console = window.console || function(t) {};
</script>
<script>
if (document.location.search.match(/type=embed/gi)) {
window.parent.postMessage("resize", "*");
}
</script>
</head>

<body translate="no">
<link rel="stylesheet" href="https://use.fontawesome.com/releases/v5.8.1/css/all.css" integrity="sha384-50oBUHEmvpQ+1lW4y57PTFmhCaXp0ML5d60M1M7uH2+nqUivzIebhndOJK28anvf" crossorigin="anonymous">
<div id="page-wrapper">
<header id="header">
<div class="logo">
<img id="header-img" src= alt="personal logo">
</div>

<nav id="nav-bar">
<ul>
<li><a class="nav-link" href="#features">Diferenciais</a></li>
<li><a class="nav-link" href="#how-it-works">Animações</a></li>
<li><a class="nav-link" href="#pricing">Preço</a></li>
</ul>
</nav>
</header>

<div class="container"></div>

<section id="hero">
<h2>Entre em contato para colaborações:</h2>
<form id="form" action="https://www.freecodecamp.com/email-submit">
<input name="email" id="email" type="email" placeholder="Submeta seu e-mail para a lista de contatos" required="">
<input id="submit" type="submit" value="Contactar" class="btn">
</form>
</section>

<div class="container">
<section id="features">
<div class="grid">
<div class="icon"><i class="fa fa-3x fa-area-chart" aria-hidden="true"></i></div>
<div class="desc">
<h2>Trasmita ideias!</h2>
<p>
Uma maneira de efetivamente animar suas ideias e transmití-las  para sua audiência.

Especialmente, para qualquer audiência técnica de qualquer nível ligando com conceitos abstratos, preferencialmente na área de STEM.
</p>
</div>
</div>
<div class="grid">
<div class="icon">
<i class="fa fa-3x fa-globe" aria-hidden="true"></i>
</div>
<div class="desc">
<h2>Operamos em qualquer região</h2>
<p>
Um dos benefícions de trabalhar conosco é que não é necessário se deslocar para entrar em contato e nos contratar. Na era digital, assumimos demandas de todo país.
</p>
</div>
</div>
<div class="grid">
<div class="icon">
<i class="fa fa-3x fa-question" aria-hidden="true"></i>
</div>
<div class="desc">
<h2>Ficou com dúvidas?</h2>
<p>
Entre em contato para demonstrações e sanar suas dúvidas. Por sermos um segmento inovador, pode ser comum ficarmos com dúvida como tramissor de informação, em como inserir esse tipo de material em suas apresentações de conteúdos. Nós ajudamos-o a se organizar nesse aspecto também!
</p>
</div>
</div>
</section>
<section id="how-it-works">
<iframe id="video" height="315" src="https://www.youtube-nocookie.com/embed/y8Yv4pnO7qc?rel=0&amp;controls=0&amp;showinfo=0" frameborder="0" allowfullscreen=""></iframe>
<video width="320" height="240" controls autoplay poster="video.jpg">
<source src="./movie/SurroundingGroupsDots6.mp4" type="video/mp4" />
Your browser does not support the video tag.
</video>
<video width="320" height="240" controls autoplay poster="video.jpg">
<source src="./movie/SurroundingGroupsDots18.mp4" type="video/mp4" />
Your browser does not support the video tag.
</video>


</section>
<section id="pricing">
<div class="product" id="tenor">
<div class="level">Conjunto de aulas</div>
<h2>R$600</h2>
<ol>
<li>lorem ipsum.</li>
<li>Lorem ipsum.</li>
<li>Lorem ipsum dolor.</li>
<li>Lorem ipsum.</li>
</ol>
<button class="btn">Selecione</button>
</div>
<div class="product" id="bass">
<div class="level">Aulas e Organização</div>
<h2>R$900</h2>
<ol>
<li>Lorem ipsum.</li>
<li>Lorem ipsum.</li>
<li>Lorem ipsum dolor.</li>
<li>Lorem ipsum.</li>
</ol>
<button class="btn">Selecione</button>
</div>
<div class="product" id="valve">
<div class="level">Suporte mensal</div>
<h2>R$1200</h2>
<ol>
<li>Lorem ipsum</li>
<li>Lorem ipsum</li>
<li>Lorem ipsum dolor.</li>
<li>Lorem ipsum.</li>
</ol>
<button class="btn">Selecione</button>
</div>
</section>
<footer>
<ul>
<li><a href="https://cdpn.io/freeCodeCamp/fullpage/RKRbwL#">Privacy</a></li>
<li><a href="https://cdpn.io/freeCodeCamp/fullpage/RKRbwL#">Terms</a></li>
<li><a href="https://cdpn.io/freeCodeCamp/fullpage/RKRbwL#">Contact</a></li>
</ul>
<span>Copyright 2021, EduSol</span>
</footer>
</div>
</div>
<script src="https://cpwebassets.codepen.io/assets/common/stopExecutionOnTimeout-1b93190375e9ccc259df3a57c1abc0e64599724ae30d7ea4c6877eb615f89387.js"></script>

<script src="https://cdn.freecodecamp.org/testable-projects-fcc/v1/bundle.js"></script>
<script id="rendered-js">
// coded by @atjonathan
// eslint-disable-next-line no-unused-vars
const projectName = 'product-landing-page';
//# sourceURL=pen.js
</script>
</body>
</html>
