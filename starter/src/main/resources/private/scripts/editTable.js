let eventBus = new EventBus('http://localhost:8888/eventbus');


eventBus.onopen = () => {
    console.log('Event bus connection opened');

    const div = document.querySelectorAll('div[contenteditable=true]');

    console.log(div);

    div.forEach(cell => {
        cell.addEventListener('keypress', function (e) {
        
        if(e.key === " "){
            e.preventDefault();
        }
        
        if (e.key === 'Enter') {
            e.preventDefault();
            console.log('Enter key pressed');

            let data = {};

            data["uid"]=cell.parentElement.parentElement.id
            data["field"]=cell.id;
            data["value"]=cell.innerHTML.trim();

            eventBus.publish("admin.edit", data);
        }
    }, false);
    });
};

eventBus.onclose = () => {
    console.log('Event bus connection closed');
};