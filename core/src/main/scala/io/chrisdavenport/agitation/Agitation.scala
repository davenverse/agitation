package io.chrisdavenport.agitation

import scala.concurrent.duration._
import cats.implicits._
import cats.effect.kernel._
import cats.effect.kernel.syntax.all._

/**
 * An Agitation can be seen as a disturbed pool of water.
 * If it is kept disturbed it can stay in that state,
 * once settled it will remain a settled system.
 */
abstract class Agitation[F[_]] {

  /**
   * The agitation has settled, it will not be agitated any more
   * if this has completed.
   */
  def settled: F[Unit]

  /**
   *  Sets the agitation period `d` and cancels any previous agitation
   *
   * @param d The duration to keep the agitation agitated for.
   */
  def agitate(d: Duration): F[Unit]
}

object Agitation {

  /**
   * Create an empty Agitation, which is agitated forever.
   */
  def create[F[_]: Temporal]: F[Agitation[F]] = for {
    buzzer <- Deferred[F, Unit]
    firstFiber: Fiber[F, Throwable, Unit] <- Concurrent[F].never.start
    state: Ref[F, Fiber[F, Throwable, Unit]] <- Ref[F].of(firstFiber)
  } yield new BaseAgitation[F](buzzer, state)

  /**
   * Create a timed agitation which will settle after a period.
   *
   * @param d The duration after which the agitation will settle.
   */
  def timed[F[_]: Temporal](d: FiniteDuration): F[Agitation[F]] =
    create.flatTap(_.agitate(d))

  private class BaseAgitation[F[_]: Temporal](
      buzzer: Deferred[F, Unit],
      state: Ref[F, Fiber[F, Throwable, Unit]]
  ) extends Agitation[F] {
    def settled: F[Unit] = buzzer.get
    def agitate(d: Duration): F[Unit] = for {
      fiber <- (
        Concurrent[F]
          .race(
            d match {
              case f: FiniteDuration => Temporal[F].sleep(f) >> buzzer.complete(())
              case _: Duration.Infinite => Spawn[F].never
            },
            buzzer.get
          )
          .void
        )
        .start
      oldFiber <- state.getAndSet(fiber)
      _ <- oldFiber.cancel
    } yield ()
  }

}
