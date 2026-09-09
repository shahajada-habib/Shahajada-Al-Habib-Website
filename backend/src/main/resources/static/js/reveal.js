(function () {
  var targets = document.querySelectorAll('.reveal');
  if (!targets.length) return;

  function revealAll() {
    targets.forEach(function (el) { el.classList.add('is-visible'); });
  }

  var reduceMotion = window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches;
  if (reduceMotion || !('IntersectionObserver' in window)) {
    revealAll();
    return;
  }

  var observer = new IntersectionObserver(function (entries) {
    entries.forEach(function (entry) {
      if (entry.isIntersecting) {
        entry.target.classList.add('is-visible');
        observer.unobserve(entry.target);
      }
    });
  }, { threshold: 0.1, rootMargin: '0px 0px -40px 0px' });

  targets.forEach(function (el) { observer.observe(el); });

  // Failsafe: if the observer never fires (background tab, odd viewport), don't
  // leave anything stuck at opacity:0.
  setTimeout(revealAll, 2500);
})();
