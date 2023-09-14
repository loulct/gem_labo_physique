let divs = document.querySelectorAll('div[contenteditable=true]');

divs.forEach(cell => {
    cell.addEventListener('keypress', function (e) {
    
    if(e.key === " "){
        e.preventDefault();
    }
    
    if (e.key === 'Enter') {
        e.preventDefault();
    }
    });
});

function validate(){
    let divs = document.querySelectorAll('div[contenteditable=true]');
    let error = document.getElementById("alert_msg");
    for (let i = 0; i < divs.length; i++) {
        let content = divs[i].innerText;
        if (content.length == 0) {
            error.innerHTML = "Les champs ne peuvent pas être vide";
            error.parentElement.removeAttribute("hidden");
            return false;
        }
    }
    return true;
}

document.getElementById("addTool").onclick = function(e) {
    e.preventDefault();
    if (validate()) {
        let brand = document.getElementById("brand").innerHTML;
        let model = document.getElementById("model").innerHTML;
        let descro = document.getElementById("descro").innerHTML;
        let idisep = document.getElementById("idisep").innerHTML;

        fetch('/private/add', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded'
            },
            body: 'brand=' + encodeURIComponent(brand)
            + '&model=' + encodeURIComponent(model)
            + '&descro=' + encodeURIComponent(descro)
            + '&idisep=' + encodeURIComponent(idisep)
        });

        window.location.href = "private/tools";
    }
}