package controllers

import anorm._
import play.api.Play.current
import play.api.db.DB
import play.api.mvc.{Action, Controller}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * Created by zoltanmaric on 05/03/16.
  */
object BookingController extends Controller {

  def createOffer(listingId: String, unitId: String, checkin: String, checkout: String) = Action.async(Future(DB.withConnection { implicit connection =>
    val id = SQL"""INSERT INTO offers (listing_id, unit_id, checkin, checkout)
         values ($listingId, $unitId, $checkin, $checkout)""".executeInsert[Option[Long]]()
    Ok(id.get.toString)
  }))
}
