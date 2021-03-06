Backdoor
=================

[![CircleCI](https://circleci.com/gh/GIVESocialMovement/backdoor.svg?style=svg)](https://circleci.com/gh/GIVESocialMovement/backdoor)
[![codecov](https://codecov.io/gh/GIVESocialMovement/backdoor/branch/master/graph/badge.svg?token=BonrHjq1rt)](https://codecov.io/gh/GIVESocialMovement/backdoor)

Backdoor is a database modification tool for team. Here are its highlights:

* __History of modification:__ we track the data before modification and the modification itself.
* __Access control on columns:__ we can allow certain persons to edit (or only-read) certain columns.
* __Computed column:__ we can show an extra column which is computed on the existing columns. An example use case is showing a secret url computed from the id column and the key column.
* __Webhook:__ we can send webhook when certain data is edited. An example use case is updating the search index when a row is updated.

Backdoor is currently used at [GIVE.asia](https://give.asia) and only supports Postgresql for now.

If you are using Backdoor, please star our repo to let newcomers know that they can trust our application. Backdoor is a database tool; trust and security are the most important aspects. Thank you!

Demo: https://backdoor-test.herokuapp.com (Username: backdoor.test.user@gmail.com, Password: Test#123)


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

A full working example is in the folder `example-project`. The example also includes how to deploy to Heroku.

### 1. project/plugins.sbt and build.sbt

Add Backdoor as an SBT plugin in `project/plugins.sbt`:

```
resolvers += Resolver.bintrayRepo("givers", "maven")
addSbtPlugin("givers.backdoor" % "sbt-backdoor" % "0.2.1")
```

And enable Backdoor in `build.sbt`:

```
lazy val root = project.in(file(".")).enablePlugins(Backdoor)
```


### 2. Define permissions, computed columns, and webhooks

Computed columns, webhooks, and fine-tuned permissions are the main strength of Backdoor. They allow Backdoor to replace custom-built administration dashboards.

You can see the full example at `test-project/src/scala/givers/backdoor/example-project`.

Minimally, it should include:

```
package exampleproject

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

### 3. Get Auth0 application

Backdoor depends on [Auth0](https://auth0.com) because we don't want to build our own authentication mechanism. Plus, [Auth0](https://auth0.com) offers a generous free plan, which is up to a thousand users. 


### 4. Specify configuration

Create the configuration file at `conf/application.conf`.

Minimally, the content should include:

```
include "backdoor.conf"  // include Backdoor's base configuration

// The module classpath that defines permissions, computed columns, and webhooks; it's the one we specified earlier.
play.modules.enabled += "exampleproject.Module"  

// The database that Backdoor uses for managing its internal states.
slick.dbs.default.db.properties.url="postgres://backdoor_example_project_dev_user:dev@localhost:5432/backdoor_example_project_dev"  
slick.dbs.default.db.characterEncoding="utf8"
slick.dbs.default.db.useUnicode=true

http.port=8000
http.origin="http://localhost:8000"
play.http.secret.key="DONT_CARE"
play.filters.cors.allowedOrigins = ["http://localhost:8000"]  # Forbid all cross-origin requests
play.filters.hosts.allowed = ["localhost:8000"]

// The database that Backdoor manages.
target.databaseUrl = "postgres://backdoor_example_project_target_user:dev@localhost:5432/backdoor_example_project_target"  

auth0.domain="YOUR_AUTH0_DOMAIN"
auth0.clientId="YOUR_AUTH0_CLIENT_ID"
auth0.clientSecret="YOUR_AUTH0_CLIENT_SECRET"
```

### 5. Run

After setting up all this, you can run Backdoor locally with `sbt run`.

For deploying to Heroku, please take a look at `example-project`. You would need to:

* Add `sbt-native-packager` and set it up with the app using `.enablePlugins(JavaAppPackaging)`
* Specify the values of environment variables in `example-project/src/main/resources/heroku.conf`

Then, you can deploy to Heroku with `git push`.

Please open a Github issue if you have any question.


Contibution
------------

### Publish a new version

1. Ensure the version in `framework-backdoor/build.sbt` and `sbt-backdoor/build.sbt` are identical and correct.
2. Ensure the version in `sbt-backdoor/src/main/givers/backdoor/sbtplugin/Backdoor.scala` is correct.
2. Run tests with `sbt test`
3. Publish with `sbt publish`
4. Verify that the artifact is published correctly at https://bintray.com/givers/maven/sbt-backdoor and https://bintray.com/givers/maven/framework-backdoor

To publish locally, please use `sbt publishM2` (instead of `sbt publishLocal`).
