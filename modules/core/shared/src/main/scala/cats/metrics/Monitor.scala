package cats.metrics

import cats.data.OptionT
import cats.effect.{Concurrent, Fiber, Ref, Resource, Temporal}
import cats.effect.implicits._
import cats.implicits._
import cats.metrics.instrument.Instrument
import cats.metrics.store.{Metric, Registry}
import fs2.Stream
import fs2.concurrent.Topic

import scala.concurrent.duration._

trait Monitor[F[_]] {
  def attach(reporter: Reporter[F]): F[Unit]
}

object Monitor {

  def apply[F[_]: Concurrent: Temporal](
      registry: Registry[F],
      flushFrequency: FiniteDuration = 5.seconds
  ): Resource[F, Monitor[F]] = {
    def buildSnapshot: F[Snapshot] = {
      def snapshotThem[A](insts: List[(String, Instrument.Aux[F, A])]): F[List[Metric[A]]] =
        insts.parTraverse { case (name, inst) => inst.getAndReset.map(Metric(name, _)) }

      registry.instruments.flatMap { instruments =>
        val counters     = snapshotThem(instruments.counters.toList)
        val gauges       = snapshotThem(instruments.gauges.toList)
        val histograms   = snapshotThem(instruments.histograms.toList)
        val chronometers = snapshotThem(instruments.chronometers.toList)
        (counters, gauges, histograms, chronometers).parMapN(Snapshot.apply)
      }
    }

    def startFlushing(topic: Topic[F, Snapshot]) =
      Stream
        .awakeEvery[F](flushFrequency)
        .evalMap(_ => buildSnapshot)
        .through(topic.publish)
        .compile
        .drain
        .start

    def initialise =
      for {
        topic      <- Topic[F, Snapshot]
        flushFiber <- startFlushing(topic)
        reporters  <- Ref[F].of(Vector.empty[Fiber[F, Throwable, Unit]])
      } yield new Impl[F](flushFiber, topic, reporters)

    Resource.make(initialise)(_.shutdown()).map(_.asInstanceOf[Monitor[F]])
  }

  private class Impl[F[_]: Concurrent](
      flushFiber: Fiber[F, Throwable, Unit],
      snapshotTopic: Topic[F, Snapshot],
      attachedFibers: Ref[F, Vector[Fiber[F, Throwable, Unit]]]
  ) extends Monitor[F] {

    def attach(reporter: Reporter[F]): F[Unit] = {
      def startReporter: F[Fiber[F, Throwable, Unit]] =
        snapshotTopic
          .subscribe(1)
          .filter(!_.isEmpty)
          .evalMap(reporter.flush)
          .compile
          .drain
          .start

      def detachReporterToken(idx: Long): F[Unit] = {
        val reporterOpt = attachedFibers.get.map(_.get(idx))
        OptionT(reporterOpt).semiflatMap(_.cancel).getOrElseF(ReporterAlreadyDetached().raiseError[F, Unit])
      }

      for {
        fiber <- startReporter
        idx <- attachedFibers.modify { fibers =>
          val idx = fibers.size
          (fibers :+ fiber, idx)
        }
        detachToken <- detachReporterToken(idx.toLong)
      } yield detachToken
    }

    def shutdown(): F[Unit] =
      for {
        _      <- flushFiber.cancel
        fibers <- attachedFibers.getAndSet(Vector.empty)
        _      <- fibers.parTraverse_(_.cancel)
      } yield ()
  }

}
