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

  def createBooking = Action.async(parse.json) {
    implicit request =>
      request.body.validate[BookingRequest].asEither match {
        case Right(bookingReq) =>
          createBookingOnly(bookingReq.booking)
            .flatMap(assignUsers(bookingReq.guests))
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

  private def createBookingOnly(booking: Booking): Future[Long] = Future {
    val Booking(listingId: String, unitId: String, checkin: String, checkout: String, headline, initialPrice, sleeps) = booking
    DB.withConnection { implicit connection =>
      SQL"""INSERT INTO bookings (listing_id, unit_id, checkin, checkout, headline, initial_price, sleeps)
         values ($listingId, $unitId, $checkin, $checkout, $headline, $initialPrice, $sleeps)""".executeInsert[Option[Long]]()
        .getOrElse(throw new RuntimeException(s"error creating booking $booking"))
    }
  }

  private def assignUsers(guests: Guests)(bookingId: Long): Future[Unit] = Future {
    val Guests(packLeader, bros) = guests
    assignUser(bookingId, isPackLeader = true)(packLeader)
    bros.foreach(assignUser(bookingId, isPackLeader = false))
  }

  private def assignUser(bookingId: Long, isPackLeader: Boolean)(user: String): Unit = DB.withConnection { implicit connection =>
    SQL"""INSERT INTO user_bookings (booking_id, user_email, committed, seen)
           values ($bookingId, $user, $isPackLeader, $isPackLeader)""".execute()
  }

  private def getUserBookings(user: String): Future[List[BookingWithId]] = Future {
    DB.withConnection { implicit connection =>
      SQL"""SELECT bookings.id, listing_id, unit_id, checkin, checkout, headline, initial_price, sleeps
            FROM bookings JOIN user_bookings on bookings.id = booking_id
            WHERE user_email = $user""".as(bookingWithIdParser.*)
    }
  }

  private def getCommitments(bookingId: Long): Future[List[Commitment]] = Future {
    DB.withConnection { implicit connection =>
      SQL"""SELECT user_email, committed FROM user_bookings where booking_id = $bookingId""".as(commitmentParser.*)
    }
  }
}

object BookingModels {
  case class Booking(listingId: String, unitId: String, checkin: String, checkout: String,
                     headline: String, initialPrice: Double, sleeps: Int)
  case class BookingWithId(id: Long, booking: Booking)

  case class Guests(packLeader: String, bros: List[String])
  case class BookingRequest(booking: Booking, guests: Guests)

  case class Commitment(user: String, committed: Boolean)
  case class GroupBooking(booking: Booking, commitments: List[Commitment])

  implicit val bookingFormat: Format[Booking] = Json.format[Booking]
  implicit val guestsFormat: Format[Guests] = Json.format[Guests]
  implicit val bookingRequest: Format[BookingRequest] = Json.format[BookingRequest]

  implicit val commitmentFormat: Format[Commitment] = Json.format[Commitment]
  implicit val groupBookingFormat: Format[GroupBooking] = Json.format[GroupBooking]

  val bookingWithIdParser = long("id") ~ str("listing_id") ~ str("unit_id") ~ str("checkin") ~ str("checkout") ~
    str("headline") ~ double("initial_price") ~ int("sleeps") map {
      case id ~ listingId ~ unitId ~ checkin ~ checkout ~ headline ~ initialPrice ~ sleeps =>
        BookingWithId(id, Booking(listingId, unitId, checkin, checkout, headline, initialPrice, sleeps))
    }

  val commitmentParser = str("user_email") ~ bool("committed") map {
    case user ~ committed => Commitment(user, committed)
  }
}
