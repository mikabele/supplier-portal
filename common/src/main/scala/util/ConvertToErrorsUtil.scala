package util

import cats.Applicative
import cats.data.{Chain, EitherT, ValidatedNec}
import cats.syntax.all._
import error.general.GeneralError

object ConvertToErrorsUtil {

  type ErrorsOr[A] = Either[Chain[GeneralError], A]

  type ValidatedNecErrorsOr[A] = ValidatedNec[GeneralError, A]

  sealed trait ToErrorsOr[F[_], T[_], A] {
    def convertToErrorsOr(value: T[A]): EitherT[F, Chain[GeneralError], A]
  }

  object instances {
    implicit def fromValidatedNec[F[_]: Applicative, A]: ToErrorsOr[F, ValidatedNecErrorsOr, A] =
      new ToErrorsOr[F, ValidatedNecErrorsOr, A] {
        override def convertToErrorsOr(value: ValidatedNecErrorsOr[A]): EitherT[F, Chain[GeneralError], A] =
          EitherT.fromEither(value.toEither.leftMap(_.toChain))
      }

    implicit def fromF[F[_]: Applicative, A]: ToErrorsOr[F, F, A] = new ToErrorsOr[F, F, A] {
      override def convertToErrorsOr(value: F[A]): EitherT[F, Chain[GeneralError], A] =
        EitherT.liftF(value).leftMap((_: Nothing) => Chain.empty[GeneralError])
    }
  }

  implicit class ToErrorsOrSyntax[F[_], T[_], A](value: T[A]) {
    def toErrorsOr(implicit E: ToErrorsOr[F, T, A]): EitherT[F, Chain[GeneralError], A] = E.convertToErrorsOr(value)
  }

}
