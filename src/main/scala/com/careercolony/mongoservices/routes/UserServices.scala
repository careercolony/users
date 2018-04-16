package com.careercolony.mongoservices.routes

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{ExceptionHandler, Directive1, Route}



import akka.stream.ActorMaterializer
import com.careercolony.mongoservices.factories.{DatabaseAccess, Credentials, User, User2, User3, ResponseStatus, BioData,
GetJobTitle, GetCompanies, Experience, Interest, SecondSignupStep, ContactInfo, Location, UpdateContactInfo, UpdateHeadline }

import spray.json.DefaultJsonProtocol

import ch.megard.akka.http.cors.CorsDirectives._
import ch.megard.akka.http.cors.CorsSettings

import akka.http.scaladsl.model.HttpMethods._
import scala.collection.immutable

import scala.collection.mutable.MutableList;
import spray.json._;

import java.util.concurrent.TimeUnit

import akka.http.scaladsl.model.ws.BinaryMessage
import akka.stream.scaladsl.Sink
import akka.http.scaladsl.model.ws.Message
import akka.stream.scaladsl.{ Source, Flow }
import akka.http.scaladsl.model.ws.UpgradeToWebSocket
import akka.http.scaladsl.model.ws.{ TextMessage, Message }


/**
object UserJsonSupport extends DefaultJsonProtocol with SprayJsonSupport {
  implicit val AddressFormats = jsonFormat2(Address)
  implicit val UserFormats = jsonFormat5(User)
  implicit val User2Formats = jsonFormat4(User2)
  implicit val User3Formats = jsonFormat8(User3)
  implicit val BiodataFormats = jsonFormat11(BioData)
  implicit val CredentialsFormats = jsonFormat2(Credentials)
  implicit val JobtitleFormats = jsonFormat1(GetJobTitle)
  implicit val ExpFormats = jsonFormat3(Experience)
  implicit val ResponseFormat = jsonFormat3(ResponseStatus.apply)

}

*/

object UserJsonSupport extends DefaultJsonProtocol with SprayJsonSupport {
  implicit val LocationFormats = jsonFormat11(Location)
  implicit val ContactInfoFormats = jsonFormat11(ContactInfo)
  implicit val UpdateContactInfoFormats = jsonFormat2(UpdateContactInfo)
  implicit val UpdateHeadlineFormats = jsonFormat9(UpdateHeadline)
  implicit val UserFormats = jsonFormat8(User)
  implicit val User2Formats = jsonFormat6(User2)
  implicit val User3Formats = jsonFormat16(User3)
  implicit val BiodataFormats = jsonFormat19(BioData)
  implicit val CredentialsFormats = jsonFormat2(Credentials)
  implicit val JobtitleFormats = jsonFormat1(GetJobTitle)
  implicit val ExpFormats = jsonFormat7(Experience)
  implicit val ResponseFormat = jsonFormat3(ResponseStatus.apply)
  implicit val InterestFormats = jsonFormat2(Interest)
  implicit val secsignupFormats = jsonFormat15(SecondSignupStep)

  
}

trait UserService extends DatabaseAccess {

  import UserJsonSupport._

  implicit val system: ActorSystem
  implicit val materializer: ActorMaterializer
  val logger = Logging(system, getClass)

  implicit def myExceptionHandler = {
    ExceptionHandler {
      case e: ArithmeticException =>
        extractUri { uri =>
          complete(HttpResponse(StatusCodes.InternalServerError,
            entity = s"Data is not persisted and something went wrong"))
        }
    }
  }

  import authentikat.jwt._

  private val tokenExpiryPeriodInDays = 1
  private val secretKey               = "super_secret_key"
  private val header                  = JwtHeader("HS256")

  
  private def securedContent = get {
    authenticated { claims =>
      complete(s"User ${claims.getOrElse("user", "")} accessed secured content!")
    }
  }

  private def authenticated: Directive1[Map[String, Any]] =
    optionalHeaderValueByName("Authorization").flatMap {
      case Some(jwt) if isTokenExpired(jwt) =>
        complete(StatusCodes.Unauthorized -> "Token expired.")
        
      case Some(jwt) if JsonWebToken.validate(jwt, secretKey) =>
        provide(getClaims(jwt).getOrElse(Map.empty[String, Any]))

      case _ => complete(StatusCodes.Unauthorized)
    }

  private def setClaims(email: String, expiryPeriodInDays: Long) = JwtClaimsSet(
    Map("user" -> email,
        "expiredAt" -> (System.currentTimeMillis() + TimeUnit.DAYS
          .toMillis(expiryPeriodInDays)))
  )

  private def getClaims(jwt: String) = jwt match {
    case JsonWebToken(_, claims, _) => claims.asSimpleMap.toOption
    case _                          => None
  }

  private def isTokenExpired(jwt: String) = getClaims(jwt) match {
    case Some(claims) =>
      claims.get("expiredAt") match {
        case Some(value) => value.toLong < System.currentTimeMillis()
        case None        => false
      }
    case None => false
  }

  def greeter: Flow[Message, Message, Any] =
  Flow[Message].mapConcat {
    case tm: TextMessage =>
      TextMessage(Source.single("Hello ") ++ tm.textStream ++ Source.single("!")) :: Nil
    case bm: BinaryMessage =>
      // ignore binary messages but drain content to avoid the stream being clogged
      bm.dataStream.runWith(Sink.ignore)
      Nil
  }
  val websocketRoute =
    path("greeter") {
      handleWebSocketMessages(greeter)
  }

/**
val database = new Database()

val measurementsWebSocketService =  
  Flow[Message]
    .collect {
      case TextMessage.Strict(text) =>
        Future.successful(text)
      case TextMessage.Streamed(textStream) =>
        textStream.runFold("")(_ + _)
          .flatMap(Future.successful)
    }
    .mapAsync(1)(identity)
    .map(InsertMessage.parse)
    .groupedWithin(1000, 1 second)
    .mapAsync(10)(database.bulkInsertAsync)
    .map(messages => InsertMessage.ack(messages.last))

val route = path("measurements") {  
  get {
    handleWebSocketMessages(measurementsWebSocketService)
  }
}

val bindingFuture = Http().bindAndHandle(route, "localhost", 8080)  

*/
  


  val settings = CorsSettings.defaultSettings.copy(allowedMethods = immutable.Seq(GET, PUT, POST, HEAD, OPTIONS))
  val userRoutes: Route = cors(settings){
    post {
      path("new-member") {
        entity(as[User]) { entity =>
          complete {
            try {
              val isPersisted: MutableList[User2] = insertRecord(entity)
              isPersisted match {
                case _: MutableList[_] =>
                  
                  var response: StringBuilder = new StringBuilder("[")
                  isPersisted.foreach(
                      x => response.append(x.toJson).append(",")
                    )
                  response.deleteCharAt(response.length - 1)
                  response.append("]"); 
                  
                  HttpResponse(StatusCodes.OK, entity = response.toString()) //data.toString())
                  case _ => HttpResponse(StatusCodes.BadRequest,
                   entity = s"User already exist")
              }
            } catch {
              case ex: Throwable =>
                logger.error(ex, ex.getMessage)
                HttpResponse(StatusCodes.InternalServerError,
                  entity = "Error while persisting data, please try again")
            }
          }
        }
      }
    } ~ path("get-user" / "memberID" / Segment) { (memberID: String) =>
      get {
        complete {
          try {
                val idAsRDD: MutableList[User3] = retrieveRecord(memberID.toInt)
                idAsRDD match {
                  case _: MutableList[_] =>
                    var response: StringBuilder = new StringBuilder("[")
                  idAsRDD.foreach(
                      x => response.append(x.toJson).append(",")
                    )

                    // If records exist
                    if(response.length > 1) response.deleteCharAt(response.length - 1);
                    response.append("]");  
                    HttpResponse(StatusCodes.OK, entity = response.toString()) 

                    case _ => HttpResponse(StatusCodes.InternalServerError,
                    entity = s"Error found for user")           
                }
          } catch {
            case ex: Throwable =>
              logger.error(ex, ex.getMessage)
              HttpResponse(StatusCodes.InternalServerError,
                entity = s"Error found for user")
          }
        }
      }
    } ~ path("login") {
      post {
         entity(as[Credentials]) { entity =>
          complete {
            try {
              val isPersisted: MutableList[User2] = login(entity)
              val claims = setClaims("flavoursoft@yahoo.com", tokenExpiryPeriodInDays)
              
              isPersisted match {
                case _: MutableList[_] =>
                  var response: StringBuilder = new StringBuilder("[")
                  isPersisted.foreach(
                      x => response.append(x.toJson).append(",")
                    )
                  
                  // If records exist
                  if(response.length > 1) response.deleteCharAt(response.length - 1);
                  response.append("]"); 
                  HttpResponse(StatusCodes.OK, entity = response.toString()) //data.toString())
                  case _ => HttpResponse(StatusCodes.BadRequest,
                   entity = s"User does not exist")
              }
            } catch {
              case ex: Throwable =>
                logger.error(ex, ex.getMessage)
                HttpResponse(StatusCodes.InternalServerError,
                  entity = "Error while persisting data, please try again")
            }
          }
        }
      }
    } ~ path("all-members") {
      get {
        complete {
          try {
            val idAsRDD: MutableList[User3] = retrieveRecords()
              //val isPersisted: MutableList[User2] = login(entity)
              //val claims = setClaims("flavoursoft@yahoo.com", tokenExpiryPeriodInDays)
              idAsRDD match {
                case _: MutableList[_] =>
                  var response: StringBuilder = new StringBuilder("[")
                  idAsRDD.foreach(
                      x => response.append(x.toJson).append(",")
                    )
                  
                  // If records exist
                  if(response.length > 1) response.deleteCharAt(response.length - 1);
                  response.append("]"); 

                  HttpResponse(StatusCodes.OK, entity = response.toString()) //data.toString())
                  case _ => HttpResponse(StatusCodes.BadRequest,
                   entity = s"User does not exist")
              }
          } catch {
            case ex: Throwable =>
              logger.error(ex, ex.getMessage)
              HttpResponse(StatusCodes.InternalServerError,
                entity = s"Data is not fetched and something went wrong")
          }
        }
      }
    } ~ path("update" / "firstname" / Segment / "email" / Segment) { (firstname: String, email: String) =>
      get {
        complete {
          try {
            val isPersisted = updateRecord1(email, firstname)
            isPersisted match {
              case true => HttpResponse(StatusCodes.Created,
                entity = s"Data is successfully persisted")
              case false => HttpResponse(StatusCodes.InternalServerError,
                entity = s"Error found for email : $email")
            }
          } catch {
            case ex: Throwable =>
              logger.error(ex, ex.getMessage)
              HttpResponse(StatusCodes.InternalServerError, entity = s"Error found for email : $email")
          }
        }
      }
    } ~ path("update1") {
      post {
         entity(as[User3]) { entity =>
          complete {
            try {
              val isPersisted = updateMember(entity)
              isPersisted match {
                case true => HttpResponse(StatusCodes.Created,
                  entity = s"Data is successfully persisted")
                case false => HttpResponse(StatusCodes.InternalServerError,
                  entity = s"Error found for email ")
              }
            } catch {
              case ex: Throwable =>
                logger.error(ex, ex.getMessage)
                HttpResponse(StatusCodes.InternalServerError, entity = s"Error found for email ")
            }
          }
        }
      }
    } ~ path("signup-steps") {
      put {
         entity(as[SecondSignupStep]) { entity =>
          complete {
            try {
              val isPersisted = updatesteps(entity)
              isPersisted match {
                case true => HttpResponse(StatusCodes.Created,
                entity = s"Data is successfully persisted")
              case false => HttpResponse(StatusCodes.InternalServerError,
                entity = s"Error found for email")
              }
            } catch {
              case ex: Throwable =>
                logger.error(ex, ex.getMessage)
                HttpResponse(StatusCodes.InternalServerError,
                  entity = "Error while persisting data, please try again")
            }
          }
        }
      }
    } ~ path("update-interest") {
      put {
         entity(as[Interest]) { entity =>
          complete {
            try {
              val isPersisted = updateInterest(entity)
              isPersisted match {
                case true => HttpResponse(StatusCodes.Created,
                entity = s"Data is successfully persisted")
              case false => HttpResponse(StatusCodes.InternalServerError,
                entity = s"Error found for email")
              }
            } catch {
              case ex: Throwable =>
                logger.error(ex, ex.getMessage)
                HttpResponse(StatusCodes.InternalServerError,
                  entity = "Error while persisting data, please try again")
            }
          }
        }
      }
    } ~ path("verify-email" / "memberID" / Segment) { (memberID: String) =>
        get {
          complete {
            try {
              val isPersisted = emailVerification(memberID.toInt)
              isPersisted match {
                case true => HttpResponse(StatusCodes.Created,
                entity = s"Data is successfully persisted")
              case false => HttpResponse(StatusCodes.InternalServerError,
                entity = s"Error found for email")
              }
            } catch {
              case ex: Throwable =>
                logger.error(ex, ex.getMessage)
                HttpResponse(StatusCodes.InternalServerError,
                  entity = "Error while persisting data, please try again")
            }
          }
        }
      
    } ~ path("get-titles" / "position" / Segment) { (position: String) =>
      get {
        complete {
          try {
            val idAsRDD: MutableList[GetJobTitle] = retrieveJobtiile(position)
            idAsRDD match {
              case _: MutableList[_] =>
                var response: StringBuilder = new StringBuilder("[")
                idAsRDD.foreach(
                    x => response.append(x.toJson).append(",")
                  )
                // If records exist
                if(response.length > 1) response.deleteCharAt(response.length - 1);
                response.append("]"); 
                HttpResponse(StatusCodes.OK, entity = response.toString())
              //case 0 => HttpResponse(StatusCodes.InternalServerError,
                //entity = s"Data is not fetched and something went wrong")
            }
          } catch {
            case ex: Throwable =>
              logger.error(ex, ex.getMessage)
              HttpResponse(StatusCodes.InternalServerError, entity = s"Error found for position : $position")
          }
        }
      }
    } ~ path("search-user" / "term" / Segment) { (term: String) =>
      get {
        complete {
          try {
            val idAsRDD: MutableList[User3] = retrieveSearchUser(term)
            idAsRDD match {
              case _: MutableList[_] =>
                var response: StringBuilder = new StringBuilder("[")
                idAsRDD.foreach(
                    x => response.append(x.toJson).append(",")
                  )
                // If records exist
                if(response.length > 1) response.deleteCharAt(response.length - 1);
                response.append("]"); 
                HttpResponse(StatusCodes.OK, entity = response.toString())
               //case 0 => HttpResponse(StatusCodes.InternalServerError,
               //entity = s"Data is not fetched and something went wrong")
            }
          } catch {
            case ex: Throwable =>
              logger.error(ex, ex.getMessage)
              HttpResponse(StatusCodes.InternalServerError, entity = s"Error found for term : $term")
          }
        }
      }
    } ~ path("get-experience" / "memberID" / Segment) { (memberID: String) =>
      get {
        complete {
          try {
                val idAsRDD: MutableList[Experience] = retrieveExperience(memberID)
                idAsRDD match {
                  case _: MutableList[_] =>
                    var response: StringBuilder = new StringBuilder("[")
                  idAsRDD.foreach(
                      x => response.append(x.toJson).append(",")
                    )
                  // If records exist
                  if(response.length > 1) response.deleteCharAt(response.length - 1);
                  response.append("]"); 
                  HttpResponse(StatusCodes.OK, entity = response.toString())  

                  case _ => HttpResponse(StatusCodes.InternalServerError,
                  entity = s"Error found for user")      
                }
          } catch {
            case ex: Throwable =>
              logger.error(ex, ex.getMessage)
              HttpResponse(StatusCodes.InternalServerError,
                entity = s"Error found for user : $memberID")
          }
        }
      }
    } ~ path("delete" / Segment / Segment ) { (field: String, value:String) =>
      get {
        complete {
          try {
            val idAsRDD = deleteRecord(field, value)
            idAsRDD match {
              case 1 => HttpResponse(StatusCodes.OK, entity = "Data is successfully deleted")
              case 0 => HttpResponse(StatusCodes.InternalServerError,
                entity = s"Data is not deleted and something went wrong")
            }
          } catch {
            case ex: Throwable =>
              logger.error(ex, ex.getMessage)
              HttpResponse(StatusCodes.InternalServerError,
                entity = s"Error found for $field : $value")
          }
        }
      }
    } ~ path("update-personal-info"){
      put{
        entity(as[UpdateContactInfo]) { entity =>
          complete {
            try {
              val isPersisted: MutableList[User3] = updateContactInfo(entity)
              isPersisted match {
                case _: MutableList[_] =>
                  
                  var response: StringBuilder = new StringBuilder("[")
                  isPersisted.foreach(
                      x => response.append(x.toJson).append(",")
                    )
                  response.deleteCharAt(response.length - 1)
                  response.append("]"); 
                  
                  HttpResponse(StatusCodes.OK, entity = response.toString()) //data.toString())
                  case _ => HttpResponse(StatusCodes.BadRequest,
                   entity = s"User already exist")
              }
            } catch {
              case ex: Throwable =>
                logger.error(ex, ex.getMessage)
                HttpResponse(StatusCodes.InternalServerError,
                  entity = "Error while persisting data, please try again")
            }
          }
        }
      }
    } ~ path("friend_request" / "myID" / Segment / "friendID" / Segment) { (myID: String, friendID: String) =>
      get {
        complete {
          try {
            val isPersisted = friendRequest(myID.toInt, friendID.toInt)
            isPersisted match {
              case 1 => HttpResponse(StatusCodes.Created,
                entity = s"Data is successfully persisted")
              case 0 => HttpResponse(StatusCodes.InternalServerError,
                entity = s"Error found for email : $myID")
            }
          } catch {
            case ex: Throwable =>
              logger.error(ex, ex.getMessage)
              HttpResponse(StatusCodes.InternalServerError, entity = s"Error found for member")
          }
        }
      }
    } ~ path("update-headline"){
      put{
        entity(as[UpdateHeadline]) { entity =>
          complete {
            try {
              val isPersisted: MutableList[User3] = updateHeadline(entity)
              isPersisted match {
                case _: MutableList[_] =>
                  
                  var response: StringBuilder = new StringBuilder("[")
                  isPersisted.foreach(
                      x => response.append(x.toJson).append(",")
                    )
                  response.deleteCharAt(response.length - 1)
                  response.append("]"); 
                  
                  HttpResponse(StatusCodes.OK, entity = response.toString()) //data.toString())
                  case _ => HttpResponse(StatusCodes.BadRequest,
                   entity = s"User already exist")
              }
            } catch {
              case ex: Throwable =>
                logger.error(ex, ex.getMessage)
                HttpResponse(StatusCodes.InternalServerError,
                  entity = "Error while persisting data, please try again")
            }
          }
        }
      }
    }
  }
}
