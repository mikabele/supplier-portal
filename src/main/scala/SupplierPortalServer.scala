import cats.effect._
import cats.syntax.all._
import conf.app.AppConf
import context.AppContext
import io.circe.config.parser
import io.circe.generic.auto._
import org.apache.logging.log4j._
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.server.{middleware, Server}

import scala.concurrent.ExecutionContext

object SupplierPortalServer extends IOApp {
  override def run(args: List[String]): IO[ExitCode] = {
    serverResource[IO]
      .use(_ => IO.never)
      .as(ExitCode.Success)
  }

  private def serverResource[F[_]: ConcurrentEffect: Timer: ContextShift]: Resource[F, Server[F]] = for {
    conf    <- Resource.eval(parser.decodePathF[F, AppConf]("app"))
    httpApp <- AppContext.setUp[F](conf)
    logger   = LogManager.getLogger("root")
    server <- BlazeServerBuilder[F]
      .withExecutionContext(ExecutionContext.global)
      .bindHttp(conf.server.port, conf.server.host)
      .withHttpApp(
        middleware.Logger
          .httpApp(logHeaders = true, logBody = true, logAction = Some { s: String => logger.info(s).pure[F] })(httpApp)
      )
      .resource

  } yield server
}
