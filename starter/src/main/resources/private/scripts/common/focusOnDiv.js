document.querySelectorAll('td').forEach(function(td) {
    td.addEventListener('click', function(e) {
        if (e.target !== this) return;
        var div = this.querySelector('div[contenteditable]');
        if (div) div.focus();
    });
});