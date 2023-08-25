window.onload = function(){
    let dt =  document.getElementById("returnDate");
    let today = new Date();

    if(dt != null){
        if(Date.parse(dt.dataset.value) < today){
            dt.style.color="red";  
        }
    }
}