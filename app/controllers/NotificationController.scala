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
    getUnseenBookingId(user).map(_.fold(NoContent)(id => Ok(id.toString)))
  }

  def setNotificationSeen(user: String, bookingId: Long) = Action.async {
    setBookingSeen(user, bookingId).map { updated =>
      Logger.info(s"Setting notification seen for $user id $bookingId updated $updated rows.")
      if (updated > 0) Ok else NotFound(s"No booking found for combination $user booking ID $bookingId")
    }
  }

  private def getUnseenBookingId(user: String): Future[Option[Long]] = Future {
    DB.withConnection { implicit connection =>
      SQL"""SELECT booking_id FROM user_bookings WHERE user_email = $user AND seen = FALSE LIMIT 1"""
        .executeQuery()
        .as(scalar[Long].singleOpt)
    }
  }

  private def setBookingSeen(user: String, bookingId: Long): Future[Int] = Future {
    DB.withConnection { implicit connection =>
      SQL"""UPDATE user_bookings SET seen = TRUE WHERE user_email = $user AND booking_id = $bookingId""".executeUpdate()
    }
  }

}
