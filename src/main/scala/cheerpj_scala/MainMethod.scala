package cheerpj_scala

case class MainMethod(
  isStatic: Boolean,
  hasArgs: Boolean
)

object MainMethod {
  implicit val instance: Ordering[MainMethod] =
    Ordering.by((x: MainMethod) => (x.isStatic, x.hasArgs)).reverse
}
