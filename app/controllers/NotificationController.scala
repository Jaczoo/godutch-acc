package controllers

import anorm.SqlParser._
import anorm._
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
    getUnseenOfferId(user).map {
      case Some(id) => Ok(id.toString)
      case None => NoContent
    }
  }

  private def getUnseenOfferId(user: String): Future[Option[Long]] = Future {
    DB.withConnection { implicit connection =>
      SQL"""SELECT offer_id FROM user_offers WHERE user_email = $user AND seen = FALSE LIMIT 1"""
        .executeQuery()
        .as(scalar[Long].singleOpt)
    }
  }

}
