---
layout: home

---

# agitation - Alarms and other Control Structures for Cancellation [![Build Status](https://travis-ci.com/ChristopherDavenport/agitation.svg?branch=master)](https://travis-ci.com/ChristopherDavenport/agitation) [![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.chrisdavenport/agitation_2.12/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.chrisdavenport/agitation_2.12)

## Quick Start

To use agitation in an existing SBT project with Scala 2.11 or a later version, add the following dependencies to your
`build.sbt` depending on your needs:

```scala
libraryDependencies ++= Seq(
  "io.chrisdavenport" %% "agitation"     % "<version>"
)
```

## Quick Example

First some imports and setup

```tut:silent
import cats.effect._
import cats.implicits._
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext
import io.chrisdavenport.agitation._
implicit val CS = IO.contextShift(ExecutionContext.global)
implicit val T = IO.timer(ExecutionContext.global)
```

Then a couple examples. Lets say we want something to race but the other action to extend that timeout
after part of the action has completed.

```tut:book
// Agitate set and then sleep longer than the agitation period
// Should always return Left which is settled occuring first
val example1 = {
  for {
    ag <- Agitation.create[IO]
    out <- Concurrent[IO].race(
      ag.settled,
      ag.agitate(2.seconds) >> Timer[IO].sleep(3.seconds)
    )
  } yield out
}

example1.unsafeRunSync

// Agitate set and then sleep for less than the agitation period
// Should always return Right.
val example2 = {
  for {
    ag <- Agitation.create[IO]
    out <- Concurrent[IO].race(
      ag.settled,
      ag.agitate(4.seconds) >> Timer[IO].sleep(3.seconds)
    )
  } yield out
}

example2.unsafeRunSync

// Agitate is never set, not very interesting but displays that
// Agitated will not complete and the timeout will win
// Should always return Right
val example3 = {
  for {
    ag <- Agitation.create[IO]
    out <- Concurrent[IO].race(
      ag.settled,
      Timer[IO].sleep(3.seconds)
    )
  } yield out
}

example3.unsafeRunSync
```