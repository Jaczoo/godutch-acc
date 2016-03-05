package controllers

import controllers.AutocompleteModels._
import play.api.Play.current
import play.api.libs.json.{Format, Json}
import play.api.libs.ws.WS
import play.api.mvc.{Action, Controller}

import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by zoltanmaric on 05/03/16.
  */
object AutocompleteController extends Controller {
  private val apiKey = current.configuration.getString("skyscanner-api-key").get
  def autocomplete(term: String) = Action.async {
    WS.url(s"http://partners.api.skyscanner.net/apiservices/hotels/autosuggest/v2/DE/EUR/en-US/$term")
      .withQueryString("apiKey" -> apiKey)
      .get()
      .map(_.json.as[AutocompleteResponse](autocompleteResponseFormat))
      .map(_.places.map(_.city_name))
      .map(Json.toJson(_))
      .map(Ok(_))
  }
}

object AutocompleteModels {
  case class Place(city_name: String)
  case class AutocompleteResponse(places: List[Place])

  implicit val placeFormat: Format[Place] = Json.format[Place]
  implicit val autocompleteResponseFormat: Format[AutocompleteResponse] = Json.format[AutocompleteResponse]
}