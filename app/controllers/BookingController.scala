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

  def createOffer(listingId: String, unitId: String, checkin: String, checkout: String, owner: String) = Action.async(parse.json) {
    implicit request =>
      request.body.validate[List[String]].asEither match {
        case Right(friends) =>
          createOfferOnly(listingId, unitId, checkin, checkout)
            .flatMap(assignUsers(owner, friends))
            .map(_ => Ok)
        case Left(error) => Future.successful(BadRequest(error.toString))
      }
  }

  private def createOfferOnly(listingId: String, unitId: String, checkin: String, checkout: String): Future[Long] = Future {
    DB.withConnection { implicit connection =>
      SQL"""INSERT INTO offers (listing_id, unit_id, checkin, checkout)
         values ($listingId, $unitId, $checkin, $checkout)""".executeInsert[Option[Long]]()
        .getOrElse(throw new RuntimeException(s"error creating offer $listingId, $unitId, $checkin, $checkout"))
    }
  }

  private def assignUsers(owner: String, friends: List[String])(offerId: Long): Future[Unit] = Future {
    assignUser(offerId, isOwner = true)(owner)
    friends.foreach(assignUser(offerId, isOwner = false))
  }

  private def assignUser(offerId: Long, isOwner: Boolean)(user: String): Unit = DB.withConnection { implicit connection =>
    SQL"""INSERT INTO user_offers (offer_id, user_email, committed, seen)
           values ($offerId, $user, $isOwner, $isOwner)""".execute ()
  }
}
