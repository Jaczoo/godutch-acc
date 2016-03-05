package controllers

import anorm.SqlParser._
import anorm._
import controllers.BookingModels._
import play.api.Play.current
import play.api.db.DB
import play.api.libs.json.{Format, Json}
import play.api.mvc.{Action, Controller}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * Created by zoltanmaric on 05/03/16.
  */
object BookingController extends Controller {

  def createBooking(listingId: String, unitId: String, checkin: String, checkout: String, owner: String) = Action.async(parse.json) {
    implicit request =>
      request.body.validate[List[String]].asEither match {
        case Right(friends) =>
          createOfferOnly(listingId, unitId, checkin, checkout)
            .flatMap(assignUsers(owner, friends))
            .map(_ => Ok)
        case Left(error) => Future.successful(BadRequest(error.toString))
      }
  }

  def getBookings(user: String) = Action.async {
    val groupBookings = for {
      bookingsWithId <- getUserBookings(user)
      groupBookings <- Future.traverse(bookingsWithId)(getGroupBooking)
    } yield groupBookings
    groupBookings.map(Json.toJson(_)).map(Ok(_))
  }

  private def getGroupBooking(bookingWithId: BookingWithId): Future[GroupBooking] =
    getCommitments(bookingWithId.id).map(GroupBooking(bookingWithId.booking, _))

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
           values ($offerId, $user, $isOwner, $isOwner)""".execute()
  }

  private def getUserBookings(user: String): Future[List[BookingWithId]] = Future {
    DB.withConnection { implicit connection =>
      SQL"""SELECT offers.id, listing_id, unit_id, checkin, checkout
            FROM offers JOIN user_offers on offers.id = offer_id
            WHERE user_email = $user""".as(bookingWithIdParser.*)
    }
  }

  private def getCommitments(offerId: Long): Future[List[Commitment]] = Future {
    DB.withConnection { implicit connection =>
      SQL"""SELECT user_email, committed FROM user_offers where offer_id = $offerId""".as(commitmentParser.*)
    }
  }
}

object BookingModels {
  case class Booking(listingId: String, unitId: String, checkin: String, checkout: String)
  case class BookingWithId(id: Long, booking: Booking)
  case class Commitment(user: String, committed: Boolean)
  case class GroupBooking(booking: Booking, commitments: List[Commitment])

  implicit val bookingFormat: Format[Booking] = Json.format[Booking]
  implicit val commitmentFormat: Format[Commitment] = Json.format[Commitment]
  implicit val groupBookingFormat: Format[GroupBooking] = Json.format[GroupBooking]

  val bookingWithIdParser = long("id") ~ str("listing_id") ~ str("unit_id") ~ str("checkin") ~ str("checkout") map {
    case id ~ listingId ~ unitId ~ checkin ~ checkout => BookingWithId(id, Booking(listingId, unitId, checkin, checkout))
  }

  val commitmentParser = str("user_email") ~ bool("committed") map {
    case user ~ committed => Commitment(user, committed)
  }
}
