//import cats.effect.{ConcurrentEffect, ContextShift, ExitCode, IO, IOApp, Resource, Timer}
//import io.circe.config.parser
//import org.http4s.server.Server
//import org.http4s.server.blaze.BlazeServerBuilder
//
//import scala.concurrent.ExecutionContext
//
//object SupplierPortalServer extends IOApp{
//  override def run(args: List[String]): IO[ExitCode] =
//    serverResource[IO]
//      .use(_ => IO.never)
//      .as(ExitCode.Success)
//
//  private def serverResource[F[_]: ContextShift: ConcurrentEffect: Timer]: Resource[F, Server[F]] = for {
//    conf    <- Resource.eval(parser.decodePathF[F, AppConf]("app"))
//    httpApp <- AppContext.setUp[F](conf)
//
//    server <- BlazeServerBuilder[F](ExecutionContext.global)
//      .bindHttp(conf.server.port, conf.server.host)
//      .withHttpApp(httpApp)
//      .resource
//
//  } yield server
//}
