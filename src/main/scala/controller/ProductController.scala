package controller

import cats.data.EitherT
import cats.effect.Concurrent
import cats.syntax.all._
import domain.user.Role
import dto.attachment._
import dto.criteria.CriteriaDto
import dto.product._
import io.circe.generic.auto._
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityCodec.{circeEntityDecoder, circeEntityEncoder}
import org.http4s.dsl.Http4sDsl
import service.{AuthenticationService, ProductService}
import util.ConvertToErrorsUtil.ToErrorsOrSyntax
import util.ConvertToErrorsUtil.instances.fromF
import util.ResponseHandlingUtil.marshalResponse

import java.util.UUID

object ProductController {

  def routes[F[_]: Concurrent](
    authenticationService: AuthenticationService[F],
    productService:        ProductService[F]
  ): HttpRoutes[F] = {
    implicit val dsl: Http4sDsl[F] = new Http4sDsl[F] {}
    import dsl._

    def addProduct(): HttpRoutes[F] = HttpRoutes.of[F] { case req @ POST -> Root / "api" / "product" =>
      val res = for {
        user    <- EitherT(authenticationService.retrieveUser(req))
        _       <- EitherT.fromEither(authenticationService.checkRole(user, Role.Manager :: Nil))
        product <- EitherT.liftF(req.as[ProductCreateDto])
        result  <- EitherT(productService.addProduct(product))
      } yield result

      marshalResponse(res.value)
    }

    def updateProduct(): HttpRoutes[F] = HttpRoutes.of[F] { case req @ PUT -> Root / "api" / "product" =>
      val res = for {
        user    <- EitherT(authenticationService.retrieveUser(req))
        _       <- EitherT.fromEither(authenticationService.checkRole(user, Role.Manager :: Nil))
        product <- EitherT.liftF(req.as[ProductUpdateDto])
        result  <- EitherT(productService.updateProduct(product))
      } yield result

      marshalResponse(res.value)
    }

    def deleteProduct(): HttpRoutes[F] = HttpRoutes.of[F] {
      case req @ DELETE -> Root / "api" / "product" / UUIDVar(id) =>
        val res = for {
          user   <- EitherT(authenticationService.retrieveUser(req))
          _      <- EitherT.fromEither(authenticationService.checkRole(user, Role.Manager :: Nil))
          result <- EitherT(productService.deleteProduct(id))
        } yield result

        marshalResponse(res.value)
    }

    def viewProducts(): HttpRoutes[F] = HttpRoutes.of[F] { case req @ GET -> Root / "api" / "product" =>
      val res = for {
        user     <- EitherT(authenticationService.retrieveUser(req))
        _        <- EitherT.fromEither(authenticationService.checkRole(user, Role.Manager :: Role.Client :: Nil))
        products <- productService.readProducts(UUID.fromString(user.id.value)).toErrorsOr
      } yield products

      marshalResponse(res.value)
    }

    def attachToProduct(): HttpRoutes[F] = HttpRoutes.of[F] {
      case req @ POST -> Root / "api" / "product" / "attachment" =>
        val res = for {
          user       <- EitherT(authenticationService.retrieveUser(req))
          _          <- EitherT.fromEither(authenticationService.checkRole(user, Role.Manager :: Nil))
          attachment <- EitherT.liftF(req.as[AttachmentCreateDto])
          result     <- EitherT(productService.attach(attachment))
        } yield result

        marshalResponse(res.value)
    }

    def removeAttachment(): HttpRoutes[F] = HttpRoutes.of[F] {
      case req @ DELETE -> Root / "api" / "product" / "attachment" / UUIDVar(id) =>
        val res = for {
          user   <- EitherT(authenticationService.retrieveUser(req))
          _      <- EitherT.fromEither(authenticationService.checkRole(user, Role.Manager :: Nil))
          result <- EitherT(productService.removeAttachment(id))
        } yield result

        marshalResponse(res.value)
    }

    def search(): HttpRoutes[F] = HttpRoutes.of[F] { case req @ POST -> Root / "api" / "product" / "search" =>
      val res = for {
        user     <- EitherT(authenticationService.retrieveUser(req))
        _        <- EitherT.fromEither(authenticationService.checkRole(user, Role.Client :: Nil))
        criteria <- EitherT.liftF(req.as[CriteriaDto])
        result   <- EitherT(productService.searchByCriteria(UUID.fromString(user.id.value), criteria))
      } yield result

      marshalResponse(res.value)
    }

    addProduct() <+> updateProduct() <+> deleteProduct <+> viewProducts <+> attachToProduct <+> search <+> removeAttachment()
  }
}
