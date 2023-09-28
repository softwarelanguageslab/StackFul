package backend

package object util {

  def timer[T](f: => T): (T, Long) = {
    val start = System.currentTimeMillis()
    val result = f
    val end = System.currentTimeMillis()
    (result, end - start)
  }

}
