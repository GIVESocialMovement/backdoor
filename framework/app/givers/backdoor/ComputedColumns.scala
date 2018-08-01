package givers.backdoor

import givers.backdoor.framework.models.ComputedColumn

abstract class ComputedColumns {
  def get(table: String): Seq[ComputedColumn]
}
