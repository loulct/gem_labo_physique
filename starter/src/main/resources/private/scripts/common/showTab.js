window.showTab = function(evt, tabName) {
    var i, table, navlink;

    table = document.getElementsByClassName("table");
    for (i = 0; i < table.length; i++) {
        table[i].style.display = "none";
    }

    navlink = document.getElementsByClassName("nav-link");
    for (i = 0; i < navlink.length; i++) {
        navlink[i].className = navlink[i].className.replace(" active", "");
    }

    document.getElementById(tabName).style.display = "block";
    evt.currentTarget.className += " active";
}