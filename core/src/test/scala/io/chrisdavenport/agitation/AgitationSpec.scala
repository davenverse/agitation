package io.chrisdavenport.agitation

import org.specs2._
import cats.effect._
import cats.effect.unsafe.IORuntime
import cats.syntax.all._

import scala.concurrent.duration._

object AgitationSpec extends mutable.Specification {

  implicit val rt = IORuntime.global

  "Agitation" should {
    "never return in an empty agitation" in {

      val test = for {
        ag <- Agitation.create[IO]
        out <- Concurrent[IO].race(
          ag.settled,
          Temporal[IO].sleep(1.second)
        )
      } yield out must_=== Right(())

      test.unsafeToFuture()
    }

    "return agitation if it settles" in {
      val test = for {
        ag <- Agitation.create[IO]
        out <- Concurrent[IO].race(
          ag.settled,
          ag.agitate(2.seconds) >> Temporal[IO].sleep(3.seconds)
        )
      } yield out must_=== Left(())

      test.unsafeToFuture()
    }
  }

}
