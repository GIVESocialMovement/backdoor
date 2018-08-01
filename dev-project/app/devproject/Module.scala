package devproject

import play.api.{Configuration, Environment}

class Module extends play.api.inject.Module {
  def bindings(environment: Environment, configuration: Configuration) = Seq(
    bind[givers.backdoor.Webhooks].toInstance(new Webhooks(configuration)),
    bind[givers.backdoor.ComputedColumns].toInstance(new ComputedColumns(configuration)),
    bind[givers.backdoor.Permissions].toInstance(new Permissions)
  )
}