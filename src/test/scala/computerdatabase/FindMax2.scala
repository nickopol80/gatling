package computerdatabase

import io.gatling.core.Predef._
import io.gatling.http.Predef._

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import scala.util.Random

class FindMax2 extends Simulation {

  //Конфиг заголовка запросов
  val httpProtocol = http
    .baseUrl("http://www.load-test.ru:1080") // Here is the root for all relative URLs
    //    .baseUrl("http://www.load-test.ru:1090") // Here is the root for all relative URLs
    .proxy(Proxy("localhost", 8888))
    .acceptLanguageHeader("ru-RU,ru;q=0.8,en-US;q=0.5,en;q=0.3")
    .upgradeInsecureRequestsHeader("1")
    .acceptEncodingHeader("gzip, deflate")
    .acceptEncodingHeader("ISO-8859-1")
    .userAgentHeader("Mozilla/5.0 (Macintosh; Intel Mac OS X 10.8; rv:16.0) Gecko/20100101 Firefox/16.0")
    .contentTypeHeader("application/x-www-form-urlencoded")
    .acceptHeader("text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8") // Here are the common headers

  val rnd = new Random()
  val now = LocalDate.now()
  val pattern = DateTimeFormatter.ofPattern("MM/dd/yyyy")
  val csvTypeOfSeats = csv("data/type_of_seat.csv").random
  val csvSeatingPreference = csv("data/seating_preference.csv").random

  def getRandomDate(startDate: LocalDate, random: Random): String = {
    startDate.plusDays(random.nextInt(30)).format(pattern)
  }
  //Сценарий
  val scn = scenario("Find_Max") // A scenario is a chain of requests and pauses
    .exec(getMainPage())
    .pause(2)
    .exec(login())
    .pause(2)
    .exec(flights())
    .pause(2)
    .exec(byTicket())
    .pause(2)
    .exec(logout())

  //Настройка профиля
  setUp(
    //разовый запуск
    scn.inject(atOnceUsers(1)).protocols(httpProtocol)

    //ступенчатый запуск
    //    scn.inject(
    //      nothingFor(3),
    //      incrementUsersPerSec(1) // интенсивность на ступень
    //        .times(10) // Количество ступеней
    //        .eachLevelLasting(20) // Длительность полки 129
    //        .separatedByRampsLasting(5) // Длительность разгона
    //        .startingFrom(1) // Начало нагрузки с
    //    )

  ).protocols(httpProtocol)
    .maxDuration(300) // общая длительность теста 1200
    .assertions(global.responseTime.max.lt(10000))

  //Операции
  def getMainPage() = {
    exec(
      http("Get Main page1")
        .get("/webtours")
        .check(status.is(200)))
      .exec(
        http("Get Main page2")
          .get("/webtours/header.html")
          .check(status.is(200)))
      .exec(
        http("Get Main page3")
          .get("/cgi-bin/welcome.pl")
          .check(status.is(200)))
      .exec(
        http("Get Main page4")
          .get("/cgi-bin/nav.pl")
          .check(status.is(200),
            css("""input[name='userSession']""", """value""").find.saveAs("userSession")))
  }

  def login() = {
    exec(
      http("Login1")
        .post("/cgi-bin/login.pl")
        .formParam("username", "gr6testuser1")
        .formParam("password", "testuser")
        .formParam("userSession", "#{userSession}")
        .check(status.is(200)))
      .exec(
        http("Login2")
          .get("/cgi-bin/nav.pl")
          .check(status.is(200)))
      .exec(
        http("Login3")
          .get("/cgi-bin/login.pl")
          .queryParam("intro", "true")
          .check(status.is(200),
            substring("Welcome")
            //          regex("<blockquote>(\\w+), <b>").is("Welcome")
            //          regex("Welcome")
          )
      )
  }
  def flights()= {
    exec(
      http("go to Flights1")
        .get("/cgi-bin/welcome.pl")
        .queryParam("page", "search")
        .check(status.is(200)))
      .exec(
        http("go to Flights2")
          .get("/cgi-bin/nav.pl")
          .queryParam("page", "menu")
          .queryParam("in", "flights")
          .check(status.is(200)))
      .exec(
        http("go to Flights3")
          .get("/cgi-bin/reservations.pl")
          .queryParam("page", "welcome")
          .check(status.is(200),
            //            bodyString.saveAs("responseBody"),
            //            regex(">(\\w+)<\\/option>").findAll.saveAs("sities")
            regex(">(\\w+)<\\/option>").findRandom.saveAs("departSity"),
            regex(">(\\w+)<\\/option>").findRandom.saveAs("arriveSity")
          )
      )
      .feed(csvSeatingPreference)
      .feed(csvTypeOfSeats)
  }

  //    var numDepartSity = "#{sities}".length

  def byTicket() = {
    exec(
      http("by_ticket1")
        .post("/cgi-bin/reservations.pl")
        .header("content-type", "application/x-www-form-urlencoded")
        .header("Referer", "http://www.load-test.ru:1080/cgi-bin/reservations.pl?page=welcome")
        .header("Proxy-Connection", "keep-alive")
        .formParam("advanceDiscount", "0")
        .formParam("depart", "#{departSity}")
        .formParam("departDate", now.plusDays(1).format(pattern))
        .formParam("arrive", "#{arriveSity}")
        .formParam("returnDate",  getRandomDate(now, rnd))
        .formParam("numPassengers", "1")
        .formParam("seatPref", "#{seatingPreference}")
        .formParam("seatType", "#{typeOfSeat}")
        .formParam("findFlights.x", "42")
        .formParam("findFlights.y", "10")
        .formParam(".cgifields", "roundtrip")
        .formParam(".cgifields", "seatType")
        .formParam(".cgifields", "seatPref")
        .check(status.is(200),
          substring("Find Flight"),
          regex("outboundFlight.*?(\\d+;\\d+;\\d+\\/\\d+\\/\\d+)").findRandom.saveAs("outboundFlight")
        )
    )
      .exec(
        http("by_ticket2")
          .post("/cgi-bin/reservations.pl")
          .header("content-type", "application/x-www-form-urlencoded")
          .header("Referer", "http://www.load-test.ru:1080/cgi-bin/reservations.pl")
          .header("Proxy-Connection", "keep-alive")
          .formParam("outboundFlight", "#{outboundFlight}")
          .formParam("numPassengers", "1")
          .formParam("advanceDiscount", "0")
          .formParam("seatType", "#{typeOfSeat}")
          .formParam("seatPref", "#{seatingPreference}")
          .formParam("findFlights.x", "42")
          .formParam("findFlights.y", "10")
          .check(status.is(200),
            substring("Payment Details")
          )
      )
      .exec(
        http("by_ticket3")
          .post("/cgi-bin/reservations.pl")
          .header("content-type", "application/x-www-form-urlencoded")
          .header("Referer", "http://www.load-test.ru:1080/cgi-bin/reservations.pl")
          .header("Proxy-Connection", "keep-alive")
          .formParam("firstName", "test")
          .formParam("lastName", "user1")
          .formParam("address1", "Lumumbi 1")
          .formParam("address2", "Moscow")
          .formParam("pass1", "test user1")
          .formParam("creditCard", "111111111111111")
          .formParam("expDate", "111")
          .formParam("oldCCOption", "")
          .formParam("numPassengers", "1")
          .formParam("seatType", "#{typeOfSeat}")
          .formParam("seatPref", "#{seatingPreference}")
          .formParam("outboundFlight", "#{outboundFlight}")
          .formParam("advanceDiscount", "0")
          .formParam("returnFlight", "")
          .formParam("JSFormSubmit", "off")
          .formParam("findFlights.x", "37")
          .formParam("findFlights.y", "10")
          .formParam(".cgifields", "saveCC")
          .check(status.is(200),
            substring("Invoice")
          )
      )
      .exec { session => println(session); session}
    //    .exec { session => println(session("responseBody").as[String]); session}
  }

  def logout() = {
    exec(
      http("logout1")
        .get("/cgi-bin/welcome.pl")
        .queryParam("signOff", "1")
        .check(status.is(200)))
      .exec(
        http("logout2")
          .get("/cgi-bin/nav.pl")
          .queryParam("in", "home")
          .check(status.is(200)))
  }
}