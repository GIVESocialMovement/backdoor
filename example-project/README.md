Backdoor's example project
------------------------

Setup the database with: `./setup/setup_db.sh` (You might need to modify the postgres options)

To run locally:
1. `sbt run` and go to http://localhost:8000
2. Test user:
  * Username: backdoor.test.user@gmail.com
  * Password: Test#123

Webhook is fired to: https://webhook.site/#/d46b3671-555a-4aa2-97b0-4aaff91ec115

Deploy to Heroku with: `git push heroku `git subtree split --prefix example-project`:master -f` (run from the root folder)

The heroku app is at: https://backdoor-test.herokuapp.com
