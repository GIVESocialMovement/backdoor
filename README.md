Backdoor
=================

[![CircleCI](https://circleci.com/gh/GIVESocialMovement/backdoor.svg?style=svg&circle-token=5a6a8be56d280b635d32252c95eed90a5f87a44e)](https://circleci.com/gh/GIVESocialMovement/backdoor)
[![codecov](https://codecov.io/gh/GIVESocialMovement/backdoor/branch/master/graph/badge.svg?token=DmQ8nPCjKF)](https://codecov.io/gh/GIVESocialMovement/backdoor)

Backdoor is a database modification tool for team. Here are its highlights:

* __History of modification:__ we track the data before modification and the modification itself.
* __Access control on columns:__ we can allow certain persons to edit (or only-read) certain columns.
* __Computed column:__ we can show an extra column which is computed on the existing columns. An example use case is showing a secret url computed from the id column and the key column.
* __Webhook:__ we can send webhook when certain data is edited. An example use case is updating the search index when a row is updated.

Backdoor is currently used at [GIVE.asia](https://give.asia) and only supports Postgresql for now.

If you are using Backdoor, please star our repo to let newcomers know that they can trust our application. Backdoor is a database tool, and trust (and security) are the most important aspects. Thank you!

Demo: https://backdoor-test.herokuapp.com


Motivation
-----------

As [GIVE.asia](https://give.asia) have a small engineering team, one of the challenges that we have faced is that an admin dashboard, which is some form of CRUD, is needed in order to enable our team to modify data.

We've quickly realised that building multiple admin dashboards for multiple data models doesn't scale well. While other database tools (as listed [here](https://wiki.postgresql.org/wiki/Community_Guide_to_PostgreSQL_GUI_Tools#Postbird)) are okay, they estange non-technical users and lack of important collaboration-esque features (e.g. history and column-level access control help prevent mistakes).

Thus, Backdoor was created to address this need.


Requirement
------------

* [Scala 2.12](https://www.scala-lang.org/)
* [SBT](https://www.scala-sbt.org/)
* [IntelliJ](https://www.jetbrains.com/idea/) with Scala plugin


Usage
------

The usage requires a certain degree of involvement. Please use IntelliJ, so it's easy to modify Scala code.

A full working example is in the folder `test-project`. The example also includes how to deploy to Heroku.

### build.sbt

Your build.sbt must:

* include Backdoor (and its asset jar) as dependencies
* specify `mainClass`
* specify the configuration file

Here's an example:

```
resolvers ++= Seq(
  Resolver.bintrayRepo("givers", "maven"),
)

libraryDependencies ++= Seq(
  "givers" %% "backdoor" % "0.1.6",
  "givers" %% "backdoor" % "0.1.6" classifier "assets",
)

run / javaOptions += "-Dconfig.resource=dev.conf"
run / fork := true  // Otherwise, the javaOptions above wouldn't work.

mainClass in (Compile, Keys.run) := Some("givers.backdoor.DevServerStart")
```

### Define permissions, computed columns, and webhooks

Computed columns, webhooks, and fine-tuned permissions are the main strength of Backdoor. They allow Backdoor to replace custom-built administration dashboards.

You can see the full example at `test-project/src/scala/givers/backdoor/testproject`.

Minimally, it should include:

```
package givers.backdoor.testproject

import play.api.{Configuration, Environment}

class OurWebhooks extends givers.backdoor.Webhooks {
  def get(table: String) = Seq.empty
}

class OurComputedColumns extends givers.backdoor.ComputedColumns {
  def get(table: String) = Seq.empty
}

class OurPermissions extends givers.backdoor.Permissions {
  val PERMISSIONS = Map(
    "backdoor.test.user@gmail.com" -> Permission(
      create = Set("*"),
      delete = Set("*"),
      perColumn = Map("*" -> Map("*" -> Scope.Write))
    )
  )

  def get(email: String): Option[Permission] = PERMISSIONS.get(email)
}

class Module extends play.api.inject.Module {
  def bindings(environment: Environment, configuration: Configuration) = Seq(
    bind[givers.backdoor.Webhooks].toInstance(new OurWebhooks(configuration)),
    bind[givers.backdoor.ComputedColumns].toInstance(new OurComputedColumns(configuration)),
    bind[givers.backdoor.Permissions].toInstance(new OurPermissions)
  )
}
```

### Get Auth0 application

Backdoor depends on Auth0 because we don't want to build our own authentication mechanism.

Auth0 offers a free plan up to thousands of users.


### Specify configuration

As seen above, we point Play's configuration to `dev.conf`, so we must create the file at `src/main/resources/dev.conf`.

Minimally, the content should include:

```
include "backdoor.conf"  // include Backdoor's base configuration

play.modules.enabled += "givers.backdoor.testproject.Module"  // The module classpath that defines permissions, computed columns, and webhooks.

play.evolutions.autoApply=true

slick.dbs.default.db.properties.url="postgres://backdoor_test_project_dev_user:dev@localhost:5432/backdoor_test_project_dev"  // The database for Backdoor
slick.dbs.default.db.characterEncoding="utf8"
slick.dbs.default.db.useUnicode=true

http.port=8000
http.origin="http://localhost:8000"
play.http.secret.key="DONT_CARE"
play.filters.cors.allowedOrigins = ["http://localhost:8000"]  # Forbid all cross-origin requests
play.filters.hosts.allowed = ["localhost:8000"]

target.databaseUrl = "postgres://backdoor_test_project_target_user:dev@localhost:5432/backdoor_test_project_target"  // The database that Backdoor manages

auth0.domain="YOUR_AUTH0_DOMAIN"
auth0.clientId="YOUR_AUTH0_CLIENT_ID"
auth0.clientSecret="YOUR_AUTH0_CLIENT_SECRET"
```

### Run

After setting up all this, you can run Backdoor locally with `sbt run`.

For deploying to Heroku, please take a look at `test-project`. You would need to:

* Add `sbt-native-packager` and set it up with the app using `.enablePlugins(JavaAppPackaging)`
* Specify `mainClass` for prod with `mainClass in Compile := Some("play.core.server.ProdServerStart")`
* Specify the values of environment variables in `test-project/src/main/resources/heroku.conf`

Then, you can deploy to Heroku with `git push`.

Please open a Github issue if you have any question.


Contibution
------------

### Publish a new version

1. Ensure the version in `build.sbt` is correct.
2. Run tests with `sbt test`
3. Publish with `sbt publish`
4. Verify that the artifact is published correctly at https://bintray.com/givers/maven/backdoor


