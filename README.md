# GEM LABO PHYSIQUE

## TODO
### Priority 1
- js script ? check if username (email) is filled -> then route /forgotpassword works
- in route /forgotpassword email.setTo should use filled username field
- signup form -> create email adress and notify (email) admins
- emails -> variable mdp & username

### Priority 2
- signup form -> add user to vertx-properties when post (don't know if vertx-properties is dynamic so maybe look for others options like jsonObjects)
- timeout session (look at SessionHandlers)
- add more tests verticles
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
