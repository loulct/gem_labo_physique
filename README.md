# GEM LABO PHYSIQUE

## TODO
### Priority 1
- add available_tools page (GET route)
- add unavailable_tools page (as another tab) (GET route)
- add add_tool page (POST route)

### Priority 2
- sends email when available_tools object becomes unvailable (to user, admin in Cc)
- if date_expired = true -> sends email to user (admin Cc)

### Priority 3
- signup form -> add user to vertx-properties when post (don't know if vertx-properties is dynamic so maybe look for others options like jsonObjects) || (maybe use a class User or something and use class Object for signup and login)
- check in forgotpassword page if email is mapped to an existing user otherwise do not send an password retrieving email
- timeout session (look at SessionHandlers)

### Priority 4
- improve java structure
- add stats (frequency of use)

### Priority 5
- optimise css files (main.css)
- improve password regex
- update README.md (how to install & how to use)
- add more tests verticles
