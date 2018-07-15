package givers.backdoor.framework.models

object Filter {
  abstract sealed class Value

  object Null extends Value
  object NotNull extends Value
  case class String(content: Predef.String) extends Value

  val STRING_PREFIX = "v-"
}

case class Filter(
  column: Column,
  values: Set[Filter.Value]
)
