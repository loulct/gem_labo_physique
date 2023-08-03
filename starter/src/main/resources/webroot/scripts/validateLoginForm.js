const btn = document.getElementById('btn');

btn.addEventListener('click', function() {
    let isValid = validateForm();

    if (isValid)
        console.log('Form is ready to submit.');
    }
);

function validateForm() {
    let formToValidate = document.getElementById('login');
    let elements = formToValidate.elements;

    for (let i = 0; i < elements.length; i++) {
        if (elements[i].type == 'text') {
            let invalid = elements[i].value.length == 0;

            console.log(invalid);
            if (invalid) {
                elements[i].setCustomValidity('L\'identifiant n\'est pas valide');
                return false;
            }else{
                elements[i].setCustomValidity('');
            }
        }

        if(elements[i].type == 'password'){
            let invalid = elements[i].value.length == 0;

            if (invalid) {
                elements[i].setCustomValidity('Le mot de passe n\'est pas valide');
                return false;
            }else{
                elements[i].setCustomValidity('');
            }
        }
    }
    return true;
}