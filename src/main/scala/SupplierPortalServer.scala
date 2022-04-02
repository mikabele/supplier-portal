import cats.effect.{Async, ExitCode, IO, IOApp, Resource}
import conf.app.AppConf
import context.AppContext
import io.circe.config.parser
import org.http4s.server.Server
import org.http4s.blaze.server.BlazeServerBuilder
import io.circe.generic.auto._

import scala.concurrent.ExecutionContext

// TODO - logger doesn't work

object SupplierPortalServer extends IOApp {
  override def run(args: List[String]): IO[ExitCode] =
    serverResource[IO]
      .use(_ => IO.never)
      .as(ExitCode.Success)

  private def serverResource[F[_]: Async]: Resource[F, Server] = for {
    conf    <- Resource.eval(parser.decodePathF[F, AppConf]("app"))
    httpApp <- AppContext.setUp[F](conf)

    server <- BlazeServerBuilder[F]
      .withExecutionContext(ExecutionContext.global)
      .bindHttp(conf.server.port, conf.server.host)
      .withHttpApp(httpApp)
      .resource

  } yield server
}
