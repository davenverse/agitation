package io.chrisdavenport.agitation

import scala.concurrent.duration._
import cats.implicits._
import cats.effect._
import cats.effect.concurrent._
import cats.effect.implicits._

/**
  * An Agitation can be seen as a disturbed pool of water. 
  * If it is kept disturbed it can stay in that state,
  * once settled it will remain a settled system.
  */
trait Agitation[F[_]]{
  // The agitation has settled, it will not be agitated any more
  // if this has completed.
  def settled: F[Unit]
  // Sets the agitation period `d` and cancels any previous agitation
  def agitate(d: Duration): F[Unit]
}

object Agitation {
  def create[F[_]: Concurrent: Timer]: F[Agitation[F]] = for {
    buzzer <- Deferred[F, Unit]
    firstFiber : Fiber[F, Unit] <- Concurrent[F].never.start
    state : Ref[F, Fiber[F, Unit]] <- Ref[F].of(firstFiber)
  } yield new Agitation[F]{
    def settled: F[Unit] = buzzer.get
    def agitate(d: Duration): F[Unit] = for {
      fiber <- (
        Concurrent[F].race(
          d match {
            case f : FiniteDuration => Timer[F].sleep(f) >> buzzer.complete(())
            case _ : Duration.Infinite => Async[F].never
          },
          buzzer.get
        ).void
      ).start
      oldFiber <- state.getAndSet(fiber)
      _ <- oldFiber.cancel
    } yield ()
  }

  def timed[F[_]: Concurrent: Timer](d: FiniteDuration): F[Agitation[F]] = 
    create.flatTap(_.agitate(d))

}