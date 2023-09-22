let eventBus = new EventBus('http://localhost:8888/eventbus');


//TODO can change multiple cell and have "Enter" key event on window rather than cell ?

eventBus.onopen = () => {
    console.log('Event bus connection opened');

    let divs = document.querySelectorAll('div[contenteditable=true]');

    let error = document.getElementById("alert_msg");

    divs.forEach(cell => {
        cell.addEventListener('keypress', function (e) {
        
        if(e.key === " "){
            e.preventDefault();
        }
        
        if (e.key === 'Enter') {
            e.preventDefault();

            let data = {};

            data["id"]=cell.parentElement.parentElement.id
            data["field"]=cell.id;
            data["value"]=cell.innerHTML.trim();

            if(cell.innerHTML.trim() != ""){
                eventBus.publish("admin.edit", data);
                window.location.href = "/private/admin";
            }else{
                error.innerHTML = "Les champs ne peuvent pas être vide";
                error.parentElement.removeAttribute("hidden");
            }
        }
    }, false);
    });
};

eventBus.onclose = () => {
    console.log('Event bus connection closed');
};