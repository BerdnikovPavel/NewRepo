import io.gatling.core.Predef._
import io.gatling.core.structure.{ChainBuilder, ScenarioBuilder}
import io.gatling.http.Predef._
import io.gatling.http.protocol.HttpProtocolBuilder

import scala.concurrent.duration.DurationInt
import scala.util.Random

class MaxPerf extends Simulation {
  val domain = "localhost:9091"
  val httpProtocol: HttpProtocolBuilder = http
    .baseUrl("http://" + domain)

  val feeder = Iterator.continually(Map("clientCode" -> Random.alphanumeric.take(10).mkString))

  object Request {
    def sendRequest: ChainBuilder = {
      feed(feeder)
        .exec(http("send_request")
          .post("/api/info/clients")
          .header("accept", "application/json")
          .header("content-type", "application/json")
          .body(StringBody("""{"clientCode": "#{clientCode}"}"""))
          .check(jsonPath("$.clientCode").is("#{clientCode}"))
          .check(status is 200))
    }
  }

  val scn = scenario("MaxPerf")
    .exec(Request.sendRequest)

  setUp(scn.inject(incrementUsersPerSec(50) //Интенсивность на ступень
    .times(6) //количество ступеней
    .eachLevelLasting(5.minutes) //длительность ступени
    .startingFrom(50))) //начальное количество vusers
    .protocols(httpProtocol)
}

