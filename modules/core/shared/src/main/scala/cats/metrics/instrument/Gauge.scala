package cats.metrics.instrument

import cats.effect.{Sync, Ref}
import cats.syntax.functor._

trait Gauge[F[_]] extends Instrument[F] {
  type Value = Double

  def set(value: Double): F[Unit]
}

object Gauge {

  def apply[F[_]: Sync](name: String, initial: Double = 0.0): F[Gauge[F]] =
    Ref[F].of(initial).map(new Impl[F](name, initial, _))

  private class Impl[F[_]: Sync](val name: String, initial: Double, value: Ref[F, Double])
      extends Gauge[F] {
    def get: F[Double]          = value.get
    def set(v: Double): F[Unit] = value.set(v)
    def reset: F[Unit]          = value.set(initial)
    def getAndReset: F[Double]  = value.getAndSet(initial)
  }

}
