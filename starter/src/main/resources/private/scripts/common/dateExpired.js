window.onload = function(){
    let dts =  document.querySelectorAll('[id=returnDate]');
    let today = new Date();

    dts.forEach(function(dt){
        if(dt != null){
            if(Date.parse(dt.dataset.value) < today){
                dt.style.color="red";  
            }
        }
    });
}