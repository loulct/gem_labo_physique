# GEM LABO PHYSIQUE

## TODO
### Priority 1
- signup form -> add user to vertx-properties when post (don't know if vertx-properties is dynamic so maybe look for others options like jsonObjects)
- signup form -> create email adress and notify (email) admins
- timeout session (look at SessionHandlers)
- mot de passe oublié ? -> sends an email to user
- adds Javascript field conditions (login page -> regex for valid email ; signup page -> strong password and valid phone number)
- update README.md (how to install & how to use)
- add more tests verticles
- use mvc (?) architecture for web resources (css in style folder, javascript in script folder, html in view folder)
- add available_tools page (GET route)
- add unavailable_tools page (as another tab) (GET route)
- add add_tool page (POST route)

### Priority 2 
- sends email when available_tools object becomes unvailable (to user, admin in Cc)
- if date_expired = true -> sends email to user (admin Cc)
- add stats (frequency of use)