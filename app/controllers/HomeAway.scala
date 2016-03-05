package controllers

import play.api.libs.ws.WS
import play.api.mvc.{Action, Controller}
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.Play.current
import scala.concurrent.Future
import scala.util.control.NonFatal

/**
  * Created by janko on 05/03/16.
  */
object HomeAway  extends Controller {
  lazy val bearer = current.configuration.getString("homeaway.bearer").get
  lazy val HomeAwayBase = "https://ws.homeaway.com/public/"
  lazy val headers = Map("Authorization" -> s"Bearer $bearer").toList

  def listings(location : String, checkInDate: String, checkOutDate: String, noOfPeople: Option[String] = None) = Action.async {
    val params = Some(Map(
      "availabilityStart" -> checkInDate,
      "availabilityEnd" -> checkOutDate,
      "q" -> location
    ))
    val extraParams = noOfPeople.map {
      peopleCount =>
        Map("minSleeps" -> peopleCount , "maxSleeps" -> peopleCount)
    }
    val endParams = (params ++ extraParams).flatten.toList
    WS.url(s"${HomeAwayBase}search").withHeaders(headers: _*).withQueryString(endParams:_*).get().map{ response =>
      Ok(response.json)
    }.recover {
      case NonFatal(x) => InternalServerError(x.getMessage)
    }
  }
}
