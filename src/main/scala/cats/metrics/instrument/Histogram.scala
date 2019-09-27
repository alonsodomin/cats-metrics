package cats.metrics.instrument

import cats.effect.{Sync, Timer}
import cats.implicits._

import org.HdrHistogram.AtomicHistogram

trait Histogram[F[_]] extends Instrument[F] {
  type Value = Distribution[Long]

  def record(value: Long): F[Unit]

}

object Histogram {

  def apply[F[_]: Timer](name: String, dynamicRange: DynamicRange = DynamicRange.Default)(
      implicit F: Sync[F]
  ): F[Histogram[F]] =
    F.delay(
        new AtomicHistogram(
          dynamicRange.lowestDiscernibleValue,
          dynamicRange.highestTrackableValue,
          dynamicRange.significantValueDigits
        )
      )
      .map(new Impl(name, _))

  private class Impl[F[_]: Timer](val name: String, hist: AtomicHistogram)(implicit F: Sync[F])
      extends Histogram[F] {
    def get: F[Distribution[Long]] =
      F.delay(Distribution(hist.getTotalCount))

    def record(value: Long): F[Unit] = F.delay(hist.recordValue(value))

  }

}