# GEM LABO PHYSIQUE

## TODO
### Priority 1
- add forgotpassword html page form (redirect href) then use route /forgotpassword on submit to send email
- forgotpassword form -> parse form-data body for email body and receiver
- signup form -> parse form-data body for email body and receiver

### Priority 2
- signup form -> add user to vertx-properties when post (don't know if vertx-properties is dynamic so maybe look for others options like jsonObjects) || (maybe use a class User or something and use class Object for signup and login)
- timeout session (look at SessionHandlers)
- add available_tools page (GET route)
- add unavailable_tools page (as another tab) (GET route)
- add add_tool page (POST route)

### Priority 3
- sends email when available_tools object becomes unvailable (to user, admin in Cc)
- if date_expired = true -> sends email to user (admin Cc)
- add stats (frequency of use)

### Priority 4
- optimise css files (main.css)
- improve password regex
- update README.md (how to install & how to use)
- add more tests verticles
