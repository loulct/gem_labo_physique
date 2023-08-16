# GEM LABO PHYSIQUE

## TODO
### Priority 1
### User/Admin & Session
- signup form -> add user to vertx-properties when post (don't know if vertx-properties is dynamic so maybe look for others options like jsonObjects)
- timeout session (look at SessionHandlers)
- handleGetTool -> use session for userEmail
- check if forgotpassword email is in "database"
- admin email parse list to email setUp (every admin account in Cc rather than admin@gem-labo.com)


### Priority 2
### Date
- if date_expired = true -> sends email to user (admin Cc)
- if return date expires -> date in red in table


### Priority 3
- add stats (frequency of use)
- add more tests verticles


### Priority 4
### Minor changes
- improve java structure
- optimise css files (main.css) + charte graphique homogénisation
- improve password regex
- clear password in browser console !!!
- update README.md (how to install & how to use)