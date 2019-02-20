package io.chrisdavenport.agitation

import org.specs2._
import cats.effect._
import cats.implicits._
// import cats.effect.laws.util.TestContext
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext
import scala.util.Success

object AgitationSpec extends mutable.Specification {

  "Agitation" should {
    "never return in an empty agitation" in {
      implicit val cs = IO.contextShift(ExecutionContext.global)
      implicit val t = IO.timer(ExecutionContext.global)

      val test = for {
        ag <- Agitation.create[IO]
        out <- Concurrent[IO].race(
          ag.settled,
          Timer[IO].sleep(1.second)
        )
      } yield out

      test.unsafeRunSync must_=== Right(())
    }

    "return agitation if it settles" in {
      implicit val ec = ExecutionContext.global
      implicit val cs = IO.contextShift(ExecutionContext.global)
      implicit val t = IO.timer(ExecutionContext.global)

      val test = for {
        ag <- Agitation.create[IO]
        out <- Concurrent[IO].race(
          ag.settled,
          ag.agitate(2.seconds) >> Timer[IO].sleep(3.seconds)
        )
      } yield out

      val testF = test.unsafeToFuture 
      (Timer[IO].sleep(3.seconds).unsafeToFuture.flatMap(_ => testF)).value must_=== Some(Success(Either.left[Unit, Unit](())))
    }
  }

}