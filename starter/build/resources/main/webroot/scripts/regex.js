const input_fields ={ 
    login:{
        username : {
            rgx: /^([a-z\d\.-]+)@([a-z\d-]+)\.([a-z]{2,8})(\.[a-z]{2,8})?$/,
            msg: 'L\'e-mail n\'est pas valide',
        },
        password : {
            rgx: /[^ ]/,
            msg: 'Le mot de passe n\'est pas valide',
        },
    },
    signup:{
        firstname: {
            rgx: /^[a-z\d]{2,20}$/i, 
            msg: 'Le prénom n\'est pas valide',
        },
        lastname: {
            rgx: /^[a-z\d]{2,20}$/i, 
            msg: 'Le nom n\'est pas valide',
        },
        phone: {
            rgx: /^\d{10}$/,
            msg: 'Le téléphone n\'est pas valide',
        },
    },
}

export {input_fields};