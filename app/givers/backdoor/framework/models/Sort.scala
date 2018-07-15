package givers.backdoor.framework.models

object Sort {
  object Direction {
    sealed abstract class Value
    object Asc extends Value {
      override def toString = "asc"
    }
    object Desc extends Value {
      override def toString = "desc"
    }
  }
}

case class Sort(
  column: Column,
  direction: Sort.Direction.Value
)
