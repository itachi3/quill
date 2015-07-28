package test.paper

import io.getquill.Source
import io.getquill.jdbc.JdbcSource
import test.Spec
import io.getquill.Partial
import io.getquill.Queryable

class PeopleSpec extends Spec {

  object peopleDB extends JdbcSource

  case class Person(name: String, age: Int)
  case class Couple(her: String, him: String)

  "Example 1 - diferences" in {

    val differences =
        for {
          c <- Queryable[Couple]
          w <- Queryable[Person]
          m <- Queryable[Person] if (c.her == w.name && c.him == m.name && w.age > m.age)
        } yield {
          (w.name, w.age - m.age)
        }

    peopleDB.run(differences) mustEqual List(("Alex", 5), ("Cora", 2))
  }

  "Example 2 - range simple" in {

    val rangeSimple = Partial {
      (a: Int, b: Int) =>
        for {
          u <- Queryable[Person] if (a <= u.age && u.age < b)
        } yield {
          u
        }
    }

    val r = rangeSimple(30, 40)

    peopleDB.run(r) mustEqual List(Person("Cora", 33), Person("Drew", 31))
  }

  "Examples 3, 4 - satisfies" in {
    val satisfies =
      Partial {
        (p: Int => Boolean) =>
          for {
            u <- Queryable[Person] if (p(u.age))
          } yield {
            u
          }
      }

    peopleDB.run(satisfies((x: Int) => 20 <= x && x < 30)) mustEqual List(Person("Edna", 21))
    peopleDB.run(satisfies((x: Int) => x % 2 == 0)) mustEqual List(Person("Alex", 60), Person("Fred", 60))
  }

  "Example 5 - compose" in {
    val range = Partial {
      (a: Int, b: Int) =>
        for {
          u <- Queryable[Person] if (a <= u.age && u.age < b)
        } yield {
          u
        }
    }

    val ageFromName = Partial {
      (s: String) =>
        for {
          u <- Queryable[Person] if (s == u.name)
        } yield {
          u.age
        }
    }

    val compose = Partial {
      (s: String, t: String) =>
        for {
          a <- ageFromName(s)
          b <- ageFromName(t)
          r <- range(a, b)
        } yield {
          r
        }
    }

    peopleDB.run(compose("Drew", "Bert")) mustEqual List(Person("Cora", 33), Person("Drew", 31))
  }

}