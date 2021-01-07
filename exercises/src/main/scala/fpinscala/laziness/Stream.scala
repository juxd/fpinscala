package fpinscala.laziness

import Stream._
trait Stream[+A] {

  def foldRight[B](z: => B)(f: (A, => B) => B): B = // The arrow `=>` in front of the argument type `B` means that the function `f` takes its second argument by name and may choose not to evaluate it.
    this match {
      case Cons(h,t) => f(h(), t().foldRight(z)(f)) // If `f` doesn't evaluate its second argument, the recursion never occurs.
      case _ => z
    }

  def exists(p: A => Boolean): Boolean = 
    foldRight(false)((a, b) => p(a) || b) // Here `b` is the unevaluated recursive step that folds the tail of the stream. If `p(a)` returns `true`, `b` will never be evaluated and the computation terminates early.

  @annotation.tailrec
  final def find(f: A => Boolean): Option[A] = this match {
    case Empty => None
    case Cons(h, t) => if (f(h())) Some(h()) else t().find(f)
  }
  def take(n: Int): Stream[A] = {
    this match {
      case Cons(h, t) if n > 1 => cons(h(), t().take(n - 1))
      case Cons(h, _) if n == 1 => cons(h(), empty)
      case _ => empty
    }
  }

  def drop(n: Int): Stream[A] = {
    this match {
      case Cons(h, t) if n > 0 => t().drop(n - 1)
      case _ => this
    }
  }

  def takeWhile(p: A => Boolean): Stream[A] = {
    this match {
      case Cons(h, t) if p(h()) => cons(h(), t().takeWhile(p))
      case _ => empty
    }
  }

  def forAll(p: A => Boolean): Boolean = foldRight(true)((s, bool) => p(s) && bool)

  def takeWhile1(p: A => Boolean): Stream[A] = foldRight(empty[A])((s, acc) => if (p(s)) cons(s, acc) else acc)
    
  def headOption: Option[A] = foldRight(None: Option[A])((h, _) => Some(h))

  def map[B](f: A => B): Stream[B] = foldRight(empty[B])((h, acc) => cons(f(h), acc))

  def filter(p: A => Boolean): Stream[A] = foldRight(empty[A])((h, acc) => if (p(h)) cons(h, acc) else acc)

  def append[B>:A](s: => Stream[B]): Stream[B] = foldRight(this)((h, acc) => cons(h, acc))

  def flatMap[B](f: A => Stream[B]): Stream[B] = foldRight(empty[B])((h, acc) => f(h) append acc)

  // 5.7 map, filter, append, flatmap using foldRight. Part of the exercise is
  // writing your own function signatures.

  def startsWith[B](s: Stream[B]): Boolean = ???

  def toList: List[A] = {
    @annotation.tailrec
    def helper(s: Stream[A], l: List[A]): List[A] = {
      s match {
        case Empty => l
        case Cons(h, t) => helper(t(), h()::l)
      }
    }
    helper(this, List()).reverse
  }
}
case object Empty extends Stream[Nothing]
case class Cons[+A](h: () => A, t: () => Stream[A]) extends Stream[A]

object Stream {
  def main(args: Array[String]): Unit = {
    val oneToFive = cons(1, cons(2, cons(3, cons(4, cons(5, empty)))))
    println(oneToFive.take(4).toList)
    println(oneToFive.take(4).drop(2).toList)
    println(oneToFive.takeWhile(_ <= 3).toList)
    println(oneToFive.forAll(_ < 6))
    println(oneToFive.forAll(_ < 5))
    println(oneToFive.takeWhile1(_ <= 3).toList)
    println(oneToFive.map(_ + 1).toList)
    println(oneToFive.filter(_ % 2 == 0).toList)
    println(oneToFive.append(oneToFive).toList)
    val threeStream: Int => Stream[Int] = ((x: Int) => cons(x, cons(x, cons(x, empty))))
    println(threeStream(3).toList)
    println(oneToFive.flatMap(threeStream).toList) // not sure about this one
    println(ones.take(5).toList)
    println(from(1).take(5).toList)
    println(fib.take(10).toList)
    println(fib1.take(10).toList)
    println(from1(1).take(5).toList)
    println(constant1(1).take(5).toList)
    println(ones1.take(5).toList)
  }
  def cons[A](hd: => A, tl: => Stream[A]): Stream[A] = {
    lazy val head = hd
    lazy val tail = tl
    Cons(() => head, () => tail)
  }

  def empty[A]: Stream[A] = Empty

  def apply[A](as: A*): Stream[A] =
    if (as.isEmpty) empty 
    else cons(as.head, apply(as.tail: _*))

  def constant[A](a: A): Stream[A] = Stream.cons(a, constant(a))

  val ones: Stream[Int] = constant(1)

  def from(n: Int): Stream[Int] = Stream.cons(n, from(n + 1))

  def fib: Stream[Int] = {
    def helper(a: Int, b: Int): Stream[Int] = {
      cons(a, helper(b, a + b))
    }
    helper(0, 1)
  }

  def unfold[A, S](z: S)(f: S => Option[(A, S)]): Stream[A] = {
    f(z) match {
      case None => empty
      case Some((a, s)) => cons(a, unfold(s)(f))
    }
  }

  // infinite: don't return None
  def fib1: Stream[Int] = unfold[Int, (Int, Int)]((0, 1)){ case(a: Int, b: Int) => Some((a, (b, a + b))) }


  // infinite: don't return None
  def from1(n: Int): Stream[Int] = unfold(n)(x => Some((x, x + 1)))

  def constant1[A](a: A): Stream[A] = unfold(a)(x => Some((x, x)))

  def ones1: Stream[Int] = unfold(1)(x => Some((x, x)))
}