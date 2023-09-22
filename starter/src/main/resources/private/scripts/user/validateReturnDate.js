let today = new Date().toISOString().split('T')[0];
document.getElementById("session-date").setAttribute('min', today);

document.querySelectorAll("#borrow").forEach(function(btn){
    let error = document.getElementById("alert_msg date");
    btn.addEventListener("click", function(e){
        modal.style.display = "block";
        document.getElementById("validate-date").onclick = function(e){
            e.preventDefault();

            if(document.getElementById("session-date").value != ""){
                let route = btn.getAttribute("value");
                let date = document.getElementById("session-date").value;

                fetch(route, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/x-www-form-urlencoded'
                    },
                    body: 'returndate=' + encodeURIComponent(date)
                });

                window.location.href = "/private/tools";
            }else{
                error.innerHTML = "Il faut saisir une date de retour du matériel";
                error.parentElement.removeAttribute("hidden");
            }
        }
    })
});

var modal = document.getElementById("modal");
var span = document.getElementsByClassName("close")[0];

span.onclick = function() {
    modal.style.display = "none";
}

window.onclick = function(event) {
    if (event.target == modal) {
        modal.style.display = "none";
    }
}