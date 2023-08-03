import {input_fields} from './regex.js';

const form = document.getElementById('helper').getAttribute('name');
const btn = document.getElementById('btn');

const validate = (field, regex) => {
    return regex.test(field.value) ? true : false;
}

btn.addEventListener('click', function() {
    let isValid = validateForm();

    if (isValid)
        console.log('Form is ready to submit.');
    }
);

function validateForm() {
    let formToValidate = document.querySelectorAll('input');

    console.log(formToValidate);

    for(let element of formToValidate){
        if(!validate(element, input_fields[form][element.attributes.name.value].rgx)){
            element.setCustomValidity(input_fields[form][element.attributes.name.value].msg);
            return false;
        }else{
            element.setCustomValidity('');
        }
    }
    return true;
}