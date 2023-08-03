const btn = document.getElementById('btn');

btn.addEventListener('click', function() {
    let isValid = validateForm();

    if (isValid)
        console.log('Form is ready to submit.');
    }
);

function validateForm() {
    let formToValidate = document.getElementById('signup');
    let elements = formToValidate.elements;

    for (let i = 0; i < elements.length; i++) {
        if (elements[i].type.includes('text', 'phone', 'email')) {
            let invalid = elements[i].value.length == 0;

            console.log(invalid);
            if (invalid) {
                elements[i].setCustomValidity('Le champ n\'est pas valide');
                return false;
            }else{
                elements[i].setCustomValidity('');
            }
        }
    }
    return true;
}