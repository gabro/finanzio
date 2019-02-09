package splitwise

import cats.effect._

object Main extends IOApp {

  def run(args: List[String]): IO[ExitCode] =
    Splitwise
      .create[IO](
        "BoIHPFS5ctOAfTiP29Ngj8PWf8OTnldwojtvSGo4",
        "zNPACt7eJfqP9WgzxrKv98FApsCernfGmO72BMV1",
      )
      .use { splitwise =>
        for {
          currentUser <- splitwise.getCurrentUser
          expenses <- splitwise.getExpenses()
          _ <- IO.delay(println(currentUser))
          _ <- IO.delay(println(expenses))
        } yield ExitCode.Success
      }

}
