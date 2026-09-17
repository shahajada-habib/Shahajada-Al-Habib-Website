(function () {
  document.querySelectorAll('[data-quote-clamp]').forEach(function (block) {
    var text = block.querySelector('.quote-clamp');
    var toggle = block.querySelector('.quote-toggle');
    if (!text || !toggle) return;

    // Only show the toggle when the clamp is actually hiding something —
    // a short quote should never grow a "read more" link with nothing to reveal.
    if (text.scrollHeight - 2 > text.clientHeight) {
      toggle.style.display = 'inline-block';
    }

    toggle.addEventListener('click', function () {
      var expanded = block.classList.toggle('is-expanded');
      toggle.textContent = expanded ? toggle.dataset.less : toggle.dataset.more;
    });
  });
})();
