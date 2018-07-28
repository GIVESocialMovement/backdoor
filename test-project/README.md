Backdoor's test project
------------------------

Setup the database with: `./setup/setup_db.sh` (You might need to modify the postgres options)

To run locally:
1. Go to the backdoor project (the parent folder) and run `sbt stage` to generate the artifacts.
2. Ensure the artifacts defined in `build.sbt` exist.
3. `sbt run` and go to http://localhost:8000
4. Test user:
  * Username: backdoor.test.user@gmail.com
  * Password: Test#123

Webhook is fired to: https://webhook.site/#/d46b3671-555a-4aa2-97b0-4aaff91ec115

Deploy to Heroku with: `git subtree push --prefix test-project heroku master`

The heroku app is at: https://backdoor-test.herokuapp.com
