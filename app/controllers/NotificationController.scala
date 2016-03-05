package controllers

import anorm.SqlParser._
import anorm._
import play.api.Logger
import play.api.Play.current
import play.api.db.DB
import play.api.mvc.{Action, Controller}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * Created by zoltanmaric on 05/03/16.
  */
object NotificationController extends Controller {
  def getNotification(user: String) = Action.async {
    getUnseenOfferId(user).map(_.fold(NoContent)(id => Ok(id.toString)))
  }

  def setNotificationSeen(user: String, offerId: Long) = Action.async {
    setOfferSeen(user, offerId).map { updated =>
      Logger.info(s"Setting notification seen for $user id $offerId updated $updated rows.")
      if (updated > 0) Ok else NotFound(s"No offer found for combination $user offer ID $offerId")
    }
  }

  private def getUnseenOfferId(user: String): Future[Option[Long]] = Future {
    DB.withConnection { implicit connection =>
      SQL"""SELECT offer_id FROM user_offers WHERE user_email = $user AND seen = FALSE LIMIT 1"""
        .executeQuery()
        .as(scalar[Long].singleOpt)
    }
  }

  private def setOfferSeen(user: String, offerId: Long): Future[Int] = Future {
    DB.withConnection { implicit connection =>
      SQL"""UPDATE user_offers SET seen = TRUE WHERE user_email = $user AND offer_id = $offerId""".executeUpdate()
    }
  }

}
