package kek

object OptionUtils {
  def sequence[T](list: List[Option[T]]): Option[List[T]] = {
    list match {
      case x if x.contains(None) => None
      case _ => Some(list.flatMap(k => List(k.get)))
    }
  }
}
