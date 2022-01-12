package computerdatabase

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

class FindMax extends Simulation {

  val httpProtocol = http
    .baseUrl("http://www.load-test.ru:1080") // Here is the root for all relative URLs
    .acceptHeader("text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8") // Here are the common headers
    .acceptEncodingHeader("gzip, deflate")
    .acceptLanguageHeader("en-US,en;q=0.5")
    .userAgentHeader("Mozilla/5.0 (Macintosh; Intel Mac OS X 10.8; rv:16.0) Gecko/20100101 Firefox/16.0")

  val scn = scenario("Find_Max") // A scenario is a chain of requests and pauses
       .exec(http("request_1")
          .get("/webtours"))
          .pause(3) // Note that Gatling has recorded real time pauses
        //    .exec(http("request_10") // Here's an example of a POST request
        //      .post("/computers")
        //      .formParam("""name""", """Beautiful Computer""") // Note the triple double quotes: used in Scala for protecting a whole chain of characters (no need for backslash)
        //      .formParam("""introduced""", """2012-05-30""")
        //      .formParam("""discontinued""", """""")
        //      .formParam("""company""", """37"""))`


      setUp(
    scn.inject(
      incrementUsersPerSec(5) // интенсивность на ступень
        .times(2) // Количество ступеней
        .eachLevelLasting(10) // Длительность полки 129
        .separatedByRampsLasting(5) // Длительность разгона
        .startingFrom(0) // Начало нагрузки с
    )
  ).protocols(httpProtocol)
    .maxDuration(60) // общая длительность теста 1200

}