package com.careercolony.mongoservices.factories

import org.neo4j.driver.v1._
import scala.concurrent.{ ExecutionContext, Future, Await }

import scala.concurrent.duration._

import reactivemongo.api.{ DefaultDB, MongoConnection, MongoDriver }

import reactivemongo.bson.{
  BSONDateTime, BSONDocument, BSONDocumentWriter, BSONDocumentReader,BSONRegex, BSONArray, Macros, document
}
import reactivemongo.api.collections.bson.BSONCollection

import com.typesafe.config.ConfigFactory
import java.security.MessageDigest

import scala.collection.mutable.MutableList;

import scala.util.{Failure, Success}


case class Counter(_id: String, seq: Int)
case class Location(city: Option[String], state:Option[String], country: Option[String], countryCode: Option[String], lat: Option[Double], lon: Option[Double], ip: Option[String], region: Option[String], regionName: Option[String], timezone: Option[String], zip: Option[String])
case class ContactInfo(address: String, city: String, state: String, country: String, email:Option[String], mobile_phone: Option[String], birth_day:Option[Int], birth_month:Option[Int], birth_year:Option[Int], twitter_profile:Option[String], facebook_profile:Option[String])
case class UpdateContactInfo(memberID: Int, contact_info: Option[ContactInfo])
case class UpdateHeadline(memberID: Int, firstname: String, lastname: String, current_employer: Option[String], current_position: Option[String], current_education: Option[String], industry: Option[String], country:Option[String], summary: Option[String])
case class User2(memberID: Int, firstname: String, lastname: String, email: String, email_verification_flag: Int, interest_flag: Int)
case class User3(memberID: Int, firstname: String, lastname: String, userIP: Option[String], email: String, contact_info: Option[ContactInfo], email_verification_flag: Int, interest_flag: Int, interest_on_colony: Option[String], country:Option[String], interest: Option[List[String]], employmentstatus: Option[String], avatar: Option[String], location: Option[String], connections: Option[List[String]], connection_requests: Option[List[String]])
case class Credentials(email: String, password: String)
case class User(firstname: String, lastname: String, email: String, password: String, contact_info: Option[ContactInfo], location:Option[Location], connections: Option[List[String]], connection_requests: Option[List[String]] )
case class Wizard(email: String, current_employment_status: String, interest: String, current_job_title: String, current_employer: String)
case class ListUser(companies: MutableList[User2])
case class ResponseStatus(status: Int, message: String, details: String)
case class GetJobTitle(position: String)
case class GetCompanies(employer: String)
case class BioData(firstname: String, lastname: String, contact_info: Option[ContactInfo], password: String, memberID: Int, userIP: Option[String],  email:String, country:String, interest: Option[List[String]], employmentstatus:String, employer:Option[String], position:Option[String], industry:Option[String], degree:Option[String], school_name:Option[String], current: Option[String], email_verification_flag: Int, interest_flag: Int, interest_on_colony: Option[String])
case class SecondSignupStep(memberID: Int, country:String, interest: Option[List[String]], employmentstatus:String, employer:Option[String], position:Option[String], industry:Option[String], degree:Option[String], school_name:Option[String], current: Option[String], email_verification_flag: Int, interest_flag: Int, interest_on_colony: Option[String], userIP: Option[String], updated_date: Option[String])
case class Experience(expID: Int, memberID: Int, employer: String, current: String, position: String, industry: String, updated_date: String)
case class Education(eduID: Int, memberID: Int, school_name: String, degree: String, updated_date: String)
case class Interest(memberID: Int, interest: Option[String])
//case class EmailVerification(memberID: Int, vcode: String)

trait DatabaseAccess {
  
  val config = ConfigFactory.load("application.conf")

  // Neo4j connection
  val neo4jUrl = config.getString("neo4j.url")
  val userName = config.getString("neo4j.userName")
  val userPassword = config.getString("neo4j.userPassword")

  
  // Mongo started

  // My settings (see available connection options)
  val mongoUri = config.getString("mongo.url")
  val username = config.getString("mongo.username")
  val password = config.getString("mongo.password")
  val database_users = config.getString("mongo.database_users")
  val database_profile = config.getString("mongo.database_profile")

  import ExecutionContext.Implicits.global // use any appropriate context

  // Connect to the database: Must be done only once per application
  val driver = MongoDriver()
  val parsedUri = MongoConnection.parseURI(mongoUri)
  val connection = parsedUri.map(driver.connection(_))

  // Database and collections: Get references
  val futureConnection = Future.fromTry(connection)
  def db: Future[DefaultDB] = futureConnection.flatMap(_.database(database_users))
  def db1: Future[DefaultDB] = futureConnection.flatMap(_.database(database_profile))

  def counterCollection = db.map(_.collection("counters"))
  def userCollection = db.map(_.collection("users"))
  def experienceCollection = db1.map(_.collection("experience"))
  def educationCollection = db1.map(_.collection("education"))
  // Write Documents: insert or update

  implicit def interestWriter: BSONDocumentWriter[Interest] = Macros.writer[Interest]
  implicit def locationWriter: BSONDocumentWriter[Location] = Macros.writer[Location]
  implicit def addressWriter: BSONDocumentWriter[ContactInfo] = Macros.writer[ContactInfo]
  implicit def userWriter: BSONDocumentWriter[User] = Macros.writer[User]
  implicit def updateuserWriter: BSONDocumentWriter[SecondSignupStep] = Macros.writer[SecondSignupStep]


  implicit def experienceWriter: BSONDocumentWriter[Experience] = Macros.writer[Experience]
  implicit def educationWriter: BSONDocumentWriter[Education] = Macros.writer[Education]
  // or provide a custom one

  def createUser(user: User): Future[Unit] =
    userCollection.flatMap(_.insert(user).map(_ => {})) // use userWriter

  def updateUser(user: User): Future[Int] = {
    val selector = document(
      "firstname" -> user.firstname,
      "lastname" -> user.lastname
    )
    // Update the matching user
    userCollection.flatMap(_.update(selector, user).map(_.n))
  }

  implicit def counterReader: BSONDocumentReader[Counter] = Macros.reader[Counter]
  implicit def interestReader: BSONDocumentReader[Interest] = Macros.reader[Interest]
  implicit def locationReader: BSONDocumentReader[Location] = Macros.reader[Location]
  implicit def addressReader: BSONDocumentReader[ContactInfo] = Macros.reader[ContactInfo]
  implicit def userReader: BSONDocumentReader[User] = Macros.reader[User]
  implicit def user2Reader: BSONDocumentReader[User2] = Macros.reader[User2]
  implicit def user3Reader: BSONDocumentReader[User3] = Macros.reader[User3]

  implicit def experienceReader: BSONDocumentReader[Experience] = Macros.reader[Experience]
  implicit def educationReader: BSONDocumentReader[Education] = Macros.reader[Education]
  
  // or provide a custom one

  def findUserByAge(age: Int): Future[List[User]] =
    userCollection.flatMap(_.find(document("age" -> age)). // query builder
      cursor[User]().collect[List]()) // collect using the result cursor
      // ... deserializes the document using userReader

  // Mongo Khatam

  def md5(s: String) = {
    MessageDigest.getInstance("MD5").digest(s.getBytes)
  }

/**
  def getNextSequence(idKey: String) = {

    val f = counterCollection.flatMap(
      _.findAndUpdate(BSONDocument("_id" -> idKey), BSONDocument (
        "$inc" -> BSONDocument("seq" -> 1 )
        ), 
        fetchNewObject = true).map(_.result[Counter]))

    var ret:Int = 0
    f.onComplete {
      case Success(counter) => ret = counter.get.seq
      case Failure(e) => e.printStackTrace
    }

    Await.result(f, 5000 millis)

    ret
  }
*/
  def getNextSequence(idKey: String) = {

    val f = counterCollection.flatMap(
      _.findAndUpdate(BSONDocument("_id" -> idKey), BSONDocument (
        "$inc" -> BSONDocument("seq" -> 1 )
        ), 
        fetchNewObject = true).map(_.result[Counter]))


    val result = Await.result(f, 5000 millis)

    val ret:Int = result match {
      case None => -1
      case Some(c: Counter) => c.seq
    }

    ret
  }


  def login(l: Credentials) = {
    
    val f = userCollection.flatMap(_.find(document(
      "email" -> l.email,
      "password" -> l.password
      )). // query builder
      cursor[User2]().collect[List]()) // collect using the result cursor
    
    val records = MutableList[User2]()

    f.onComplete {
        case Success(result) => 
          result.foreach(
            record => {
              val user: User2 = new User2(
                record.memberID, 
                record.firstname, 
                record.lastname, 
                record.email,
                record.email_verification_flag,
                record.interest_flag
              )
              records += user
            }
          )
        case Failure(e) => { 
          e.printStackTrace
          println("Error in login..")
        }
    }

    Await.result(f, 1000 millis);

    records
  }

  def insertRecord(user: User): MutableList[User2] = {

    val records = MutableList[User2]()
   
    // Check if user exist
    var numUsers: Int = 0

    val future = userCollection.flatMap(_.find(document("email" -> user.email)). 
      cursor[User]().collect[List]()) // findUserByEmail(user.email)

    future.onComplete {
      case Success(response) => {
        numUsers = response.length
      }
      case Failure(e) => { 
        e.printStackTrace
        println("Error in getting user...")
      }
    }

    Await.result(future, 5000 millis);

    println("numUsers: " + numUsers)

    if(numUsers != 0) {
      println(user.email + ": user already exists!!!")
    }
    else {

      val location = user.location match {
        case Some(l) => {
          BSONDocument(
            "city" -> l.city,
            "state" -> l.state,
            "country" -> l.country,
            "lat" -> l.lat,
            "lon" -> l.lon,
            "ip" -> l.ip,
            "region" -> l.region,
            "timezone" -> l.timezone,
            "zip" -> l.zip

          )
        }
        case None => {
          BSONDocument()
        }
      }
      val userDoc = BSONDocument (
        "firstname" -> user.firstname, 
        "lastname" -> user.lastname, 
        "email" -> user.email, 
        "password" -> user.password, 
        "email_verification_flag" -> 0, 
        "interest_flag" -> 0, 
        "memberID" -> getNextSequence("memberID"),
        "location" -> location
       // "connections"->user.connections,
        //"connection_requests"->user.connection_requests
      )
//val cmd = Seq("curl", "-L", "-X", "POST", "-H", "'Content-Type: application/json'", "-d " + jsonHash,  args.chronosHost + "/scheduler/" + jobType)
//val cd = Seq("curl --request POST --url https://api.sendgrid.com/v3/mail/send --header "Authorization: Bearer $SENDGRID_API_KEY" --header 'Content-Type: application/json' --data '{"personalizations": [{"to": [{"email": "carlnjoku@yahoo.com.com"}]}],"from": {"email": "test@example.com"},"subject": "Sending with SendGrid is Fun","content": [{"type": "text/plain", "value": "and easy to do anywhere, even with cURL"}]}')
      println("memberID")
      println(userDoc.getAs[Int]("memberID"))
      val mem_id = userDoc.getAs[Int]("memberID")
  
      val m_id = mem_id match { case None => "" case Some(str) => str }
      //val addr = address match { case None => "" case Some(str) => str }
      val inst = userCollection.flatMap(_.insert(userDoc).map(_.n)) //.map(_ => {})) // use userWriter

      
       //Insert member details to neo4j database
       val driver = GraphDatabase.driver(neo4jUrl, AuthTokens.basic(userName, userPassword))
       val session = driver.session
       val script = s"CREATE (s:users {memberID:${m_id}, firstname:'${ user.firstname }', lastname:'${ user.lastname }', email:'${ user.email }', password:'${user.password}', signup_date: TIMESTAMP()}) "
       val result: StatementResult = session.run(script)
       session.close()
       driver.close()
       result.consume().counters().containsUpdates()
          
      var n: Int = 0
      inst.onComplete {
        
         
        case Success(value) => {
          n = value
          
        }
        case Failure(e) => e.printStackTrace
      }

      Await.result(inst, 5000 millis)  

      val f = userCollection.flatMap(_.find(document(
        "email" -> user.email
      )). // query builder
      cursor[User2]().collect[List]()) // collect using the result cursor

      f.onComplete {
        case Success(result) => 
          result.foreach (
            record => {
            val user: User2 = new User2(
              record.memberID, 
              record.firstname, 
              record.lastname, 
              record.email,
              record.interest_flag,
              record.email_verification_flag
            )
            
            records += user
          }) 
        case Failure(e) => { 
          e.printStackTrace
          println("Error in getting user...")
        }
      }

      Await.result(f, 5000 millis)
    }

    records
  }

  def retrieveRecord(memberID: Int): MutableList[User3] = {

    val f = userCollection.flatMap(_.find(document(
      "memberID" -> memberID
      )). // query builder
      cursor[User3]().collect[List]()) // collect using the result cursor
    
    val records = MutableList[User3]()

    f.onComplete {
        case Success(result) => 
          result.foreach (
            record => {

              val user: User3 = new User3(
                record.memberID,
                record.firstname, 
                record.lastname, 
                record.userIP,
                record.email, 
                record.contact_info,
                record.email_verification_flag,
                record.interest_flag,
                record.interest_on_colony,
                record.country,
                record.interest, 
                record.employmentstatus, 
                record.avatar,
                record.location,
                record.connections,
                record.connection_requests
              )

              records += user
            }
          )
        case Failure(e) => { 
          e.printStackTrace
          println("Error retieving account details..")
        }
    }

    Await.result(f, 1000 millis);
    records
    
  }

  def updateContactInfo(c: UpdateContactInfo): MutableList[User3] = {
    
    val memberID = c.memberID
    val contact_info = c.contact_info

    val selector = BSONDocument("memberID" -> c.memberID)

    val modifier = BSONDocument("$set" -> BSONDocument("contact_info"-> contact_info))
/**
    val address = c.address
    val city = c.city
    val state = c.state
    val country = c.country
    val email = c.email
    val mobile_phone = c.mobile_phone
    val birth_day = c.birth_day
    val birth_month = c.birth_month
    val birth_year = c.birth_year
    val twitter_profile = c.twitter_profile
    val facebook_profile = c.facebook_profile


    val modifier = BSONDocument( "$set" -> BSONDocument( 
    "address"-> address, 
    "city"->city,
    "state"->state, 
    "country"->country,
    "email"->email,
    "mobile_phone"->mobile_phone,
    "birth_day"->birth_day,
    "birth_month"->birth_month,
    "birth_year"->birth_year,
    "twitter_profile"->twitter_profile,
    "facebook_profile"->facebook_profile
    ))
    */
    
    /**
    val selector = BSONDocument("memberID" -> p.memberID)
    val modifier = BSONDocument (
      "address" -> c.address,
      "city" -> c.city,
      "state" -> c.state, 
      "country" -> c.country, 
      "email" -> c.email,
      "mobile_phone" -> c.mobile_phone,
      "birth_day" -> c.birth_day,
      "birth_month" -> c.birth_month,
      "birth_year" -> c.birth_year,
      "twitter_profile" -> c.twitter_profile,
      "facebook_profile" -> c.facebook_profile
    )

    */

    println(c.memberID)
    
    val inst = userCollection.flatMap(
      _.update(selector, modifier).map(_.n))

    var n: Int = 0
      inst.onComplete {
        case Success(value) => {
          n = value 
        }
        case Failure(e) => e.printStackTrace
      }

      Await.result(inst, 1000 millis)  

       val f = userCollection.flatMap(_.find(document(
        "memberID" -> c.memberID
        )). // query builder
        cursor[User3]().collect[List]()) // collect using the result cursor
       
       val records = MutableList[User3]()
      
      f.onComplete {
        case Success(result) => 
          result.foreach (
            record => {
              val user: User3 = new User3(record.memberID, 
                record.firstname, 
                record.lastname, 
                record.userIP,
                record.email, 
                record.contact_info,
                record.email_verification_flag,
                record.interest_flag,
                record.interest_on_colony,
                record.country,
                record.interest, 
                record.employmentstatus, 
                record.avatar,
                record.location,
                record.connections,
                record.connection_requests
                )

              records += user
            }
          )
        case Failure(e) => { 
          e.printStackTrace
          None
        }
      }

      Await.result(f, 1000 millis);
      records

  }

  def updateHeadline(c: UpdateHeadline): MutableList[User3] = {
    
    val memberID = c.memberID
    val firstname = c.firstname
    val lastname = c.lastname
    val current_employer = c.current_employer
    val current_position = c.current_position
    val current_education = c.current_education
    val industry = c.industry
    val country = c.country
    val summary = c.summary

    val selector = BSONDocument("memberID" -> memberID)

    val modifier = BSONDocument( "$set" -> BSONDocument( 
    "firstname"-> firstname, 
    "lastname"->lastname,
    "current_employer"->current_employer, 
    "current_position"->current_position,
    "current_education"->current_education,
    "industry"->industry,
    "country"->country,
    "summary"->summary
    ))
  

    println(c.memberID)
    
    val inst = userCollection.flatMap(
      _.update(selector, modifier).map(_.n))

    var n: Int = 0
      inst.onComplete {
        case Success(value) => {
          n = value 
        }
        case Failure(e) => e.printStackTrace
      }

      Await.result(inst, 1000 millis)  

       val f = userCollection.flatMap(_.find(document(
        "memberID" -> c.memberID
        )). // query builder
        cursor[User3]().collect[List]()) // collect using the result cursor
       
       val records = MutableList[User3]()
      
      f.onComplete {
        case Success(result) => 
          result.foreach (
            record => {
              val user: User3 = new User3(record.memberID, 
                record.firstname, 
                record.lastname, 
                record.userIP,
                record.email, 
                record.contact_info,
                record.email_verification_flag,
                record.interest_flag,
                record.interest_on_colony,
                record.country,
                record.interest, 
                record.employmentstatus, 
                record.avatar,
                record.location,
                record.connections,
                record.connection_requests
                )

              records += user
            }
          )
        case Failure(e) => { 
          e.printStackTrace
          None
        }
      }

      Await.result(f, 1000 millis);
      records

  }


  def retrieveExperience(memberID: String): MutableList[Experience] = {

    val f = experienceCollection.flatMap(_.find(document(
      "memberID" -> memberID
      )). // query builder
      cursor[Experience]().collect[List]()) // collect using the result cursor
    
    val records = MutableList[Experience]()

    f.onComplete {
        case Success(result) => 
          result.foreach(
            record => {

              val exp: Experience = new Experience(
                record.memberID,
                record.expID, 
                record.employer, 
                record.position,
                record.current,
                record.industry,
                record.updated_date)

              records += exp
            }
          )
        case Failure(e) => { 
          e.printStackTrace
          println("Error in retrieveRecord..")
        }
    }

    Await.result(f, 1000 millis);
    records
  }
  
  def retrieveRecords() =  {

    val f = userCollection.flatMap(_.find(document(
      )). // query builder
      cursor[User3]().collect[List]()) // collect using the result cursor
    
    val records = MutableList[User3]()

    f.onComplete {
        case Success(result) => 
          result.foreach (
            record => {
              val user: User3 = new User3(record.memberID, 
                record.firstname, 
                record.lastname, 
                record.userIP,
                record.email, 
                record.contact_info,
                record.email_verification_flag,
                record.interest_flag,
                record.interest_on_colony,
                record.country,
                record.interest, 
                record.employmentstatus, 
                record.avatar,
                record.location,
                record.connections,
                record.connection_requests
              )

              records += user
            }
          )
        case Failure(e) => { 
          e.printStackTrace
          None
        }
    }

    Await.result(f, 1000 millis);
    println("Returning: " + records)
    records
  }

  def retrieveSearchUser(term: String): MutableList[User3] = {
   
    val f = userCollection.flatMap(_.find(document(
      BSONDocument("firstname" -> BSONRegex(term, "i"))
      )). // query builder
      cursor[User3]().collect[List]()) // collect using the result cursor
    
    val records = MutableList[User3]()

    f.onComplete {
        case Success(result) => 
          result.foreach (
            record => {
              val user: User3 = new User3(record.memberID, 
                record.firstname, 
                record.lastname, 
                record.userIP,
                record.email, 
                record.contact_info,
                record.email_verification_flag,
                record.interest_flag,
                record.interest_on_colony,
                record.country,
                record.interest, 
                record.employmentstatus, 
                record.avatar,
                record.location,
                record.connections,
                record.connection_requests
              )

              records += user
            }
          )
        case Failure(e) => { 
          e.printStackTrace
          None
        }
    }

    Await.result(f, 1000 millis);
    println("Returning: " + records)
    records

  } 

  def updateMember(u: User3): Boolean = {
   
    val selector = BSONDocument("email" -> u.email)

    val modifier = BSONDocument (
      "memberID" -> u.memberID,
      "email" -> u.email,
      "firstname" -> u.firstname,
      "contact_info"->u.contact_info
      )
    println(u.email)
    val f = userCollection.flatMap(
      _.update(selector, modifier).map(_.n))

    var ret:Int = 0
    f.map(n => {
        ret = n
      }
    )
    

    Await.result(f, 1000 millis)

    if(ret > 0) true else false

  }

  def updateRecord1(email: String, firstname: String): Boolean = {

    val selector = BSONDocument("email" -> email)

    
    val modifier = BSONDocument (
      "email" -> email,
      "firstname" -> firstname
      )

      //case class User3(memberID: Int, firstname: String, lastname: String, 
      //email: String, address: Option[Address], interest: Option[String], 
      //employmentstatus: Option[String], avatar: Option[String])

    val f = userCollection.flatMap(
      _.update(selector, modifier).map(_.n))

    var ret:Int = 0
    f.map(n => {
        ret = n
      }
    )
    

    Await.result(f, 1000 millis)

    if(ret > 0) true else false

  }
/**
  def updatesteps(b:BioData): Boolean = {

    val employmentstatus = b.employmentstatus

    val useripVal: String = b.userIP match { case None => "" case Some(str) => str }
    val degreeVal: String = b.degree match { case None => "" case Some(str) => str }
    val school_nameVal: String = b.school_name match { case None => "" case Some(str) => str}
    val employer_nameVal: String = b.employer_name match { case None => "" case Some(str) => str}
    val positionVal : String = b.position match { case None => "" case Some(str) => str}
    val industryVal : String = b.industry match { case None => "" case Some(str) => str}

    
    if(employmentstatus == "1") {

      val script = s"MATCH (s:Members) where s.email ='${b.email}' SET s.country = '${b.country}', s.userIP = '$useripVal' , s.interest ='${b.interest}', s.employmentstatus= ${b.employmentstatus} MERGE (id:UniqueId{name:'Experience'}) ON CREATE SET id.count = 1 ON MATCH SET id.count = id.count + 1 WITH toInt(id.count) AS expid CREATE (b:Experience { expID:expid, memberID:${ b.memberID }, current:'yes', position:'$positionVal', industry:'$industryVal', employer_name:'$employer_nameVal', updated_date: TIMESTAMP() }) WITH b.MemberID AS memberid  CREATE(s)-[:WORKING_AT]->(b)  RETURN b.memberID" +
        s" AS memberID"
      val result = session.run(script)
      session.close()
      driver.close()
      result.consume().counters().containsUpdates()
    } else if(employmentstatus == "2"){
      val script = s"MATCH (s:Members) where s.email ='${b.email}' SET s.country = '${b.country}', s.userIP = '$useripVal' , s.interest ='${b.interest}', s.employmentstatus= ${b.employmentstatus} MERGE (id:UniqueId{name:'Experience'}) ON CREATE SET id.count = 1 ON MATCH SET id.count = id.count + 1 WITH toInt(id.count) AS expid CREATE (b:Experience { expID:expid, memberID:'${ b.memberID }', current:'yes', position:'$positionVal', industry:'$industryVal', employer_name:'$employer_nameVal', updated_date: TIMESTAMP() })  RETURN b.memberID" +
        s" AS memberID"
      val result = session.run(script)
      session.close()
      driver.close()
      result.consume().counters().containsUpdates()
    } else if(employmentstatus == "3"){
      val script = s"MATCH (s:Members) where s.email ='${b.email}' SET s.country = '${b.country}', s.userIP = '$useripVal' , s.interest ='${b.interest}', s.employmentstatus= ${b.employmentstatus} MERGE (id:UniqueId{name:'Experience'}) ON CREATE SET id.count = 1 ON MATCH SET id.count = id.count + 1 WITH toInt(id.count) AS expid CREATE (b:Experience { expID:expid, memberID:'${ b.memberID }', current:'yes', position:'$positionVal', industry:'$industryVal', employer_name:'$employer_nameVal', updated_date: TIMESTAMP() })  RETURN b.memberID" +
        s" AS memberID"
      val result = session.run(script)
      session.close()
      driver.close()
      result.consume().counters().containsUpdates()
    } else if(employmentstatus == "4"){
      val script = s"MATCH (s:Members) where s.email ='${b.email}' SET s.country = '${b.country}', s.userIP = '$useripVal' , s.interest ='${b.interest}', s.employmentstatus= ${b.employmentstatus} MERGE (id:UniqueId{name:'Experience'}) ON CREATE SET id.count = 1 ON MATCH SET id.count = id.count + 1 WITH toInt(id.count) AS expid CREATE (b:Experience { expID:expid, memberID:'${ b.memberID }', current:'yes', position:'$positionVal', industry:'$industryVal', employer_name:'$employer_nameVal', updated_date: TIMESTAMP() })  RETURN b.memberID" +
        s" AS memberID"
      val result = session.run(script)
      session.close()
      driver.close()
      result.consume().counters().containsUpdates()
    } else if(employmentstatus == "5"){
      val script = s"MATCH (s:Members) where s.email ='${b.email}' SET s.country = '${b.country}', s.userIP = '$useripVal' , s.interest ='${b.interest}', s.employmentstatus= ${b.employmentstatus} MERGE (id:UniqueId{name:'Experience'}) ON CREATE SET id.count = 1 ON MATCH SET id.count = id.count + 1 WITH toInt(id.count) AS expid CREATE (b:Experience { expID:expid, memberID:'${ b.memberID }', position:'$positionVal', industry:'$industryVal', employer_name:'$employer_nameVal', updated_date: TIMESTAMP() })  RETURN b.memberID" +
        s" AS memberID"
      val result = session.run(script)
      session.close()
      driver.close()
      result.consume().counters().containsUpdates()
    } else{
      val script = s"MATCH (s:Members) where s.email ='${b.email}' SET s.country = '${b.country}', s.userIP = '$useripVal' , s.interest ='${b.interest}', s.employmentstatus= ${b.employmentstatus} MERGE (id:UniqueId{name:'Education'}) ON CREATE SET id.count = 1 ON MATCH SET id.count = id.count + 1 WITH toInt(id.count) AS eduid CREATE (b:Education { eduID:eduid, memberID:'${ b.memberID }', degree:'$degreeVal', school_name:'$school_nameVal', updated_date: TIMESTAMP() })  RETURN b.memberID" +
        s" AS memberID"
      val result = session.run(script)
      session.close()
      driver.close()
      result.consume().counters().containsUpdates()
    }
    

    

  }
*/

def updatesteps(b:SecondSignupStep): Boolean = {

    val employmentstatus = b.employmentstatus
    val interest_on_colony = b.interest_on_colony
    val country = b.country
    val userIP = b.userIP


    val useripVal: String = b.userIP match { case None => "" case Some(str) => str }
    val degreeVal: String = b.degree match { case None => "" case Some(str) => str }
    val school_nameVal: String = b.school_name match { case None => "" case Some(str) => str}
    val employerVal: String = b.employer match { case None => "" case Some(str) => str}
    val positionVal : String = b.position match { case None => "" case Some(str) => str}
    val industryVal : String = b.industry match { case None => "" case Some(str) => str}


    val userdoc = BSONDocument( "$set" -> BSONDocument(
      "employmentstatus"-> employmentstatus,
      "current_position"->positionVal,
      "current_employer"->employerVal,
      "current_industry"->industryVal,
      "interest_on_colony"->interest_on_colony,
      "country"->country,
      "userIP"->userIP

    ))


  val selector = BSONDocument("memberID" -> b.memberID)

    println(employmentstatus)
    println(b.country)
    val f = userCollection.flatMap(
      _.update(selector, userdoc).map(_.n))
    
    // Update user details in neo4j
    // val driver = GraphDatabase.driver(neo4jUrl, AuthTokens.basic(userName, userPassword))
    // val session = driver.session
    // val script = s"MATCH (s:Members) where s.email ='${b.email}' SET s.country = '${b.country}', s.userIP = '$useripVal' , s.interest ='${b.interest}', s.employmentstatus= ${b.employmentstatus} "
    // val res = session.run(script)
    // session.close()
    // driver.close()

    
    
    val result = Await.result(f, 5000 millis)

    if(result > 0) {

      val inst = if(employmentstatus == "1") {
        val mergeDoc = BSONDocument (
          "expID" -> getNextSequence("expID"), 
          "memberID" -> b.memberID, 
          "current" -> b.current,
          "position" -> positionVal, 
          "industry" -> industryVal, 
          "employer" -> employerVal,
          "updated_date" -> b.updated_date
        )
        experienceCollection.flatMap(_.insert(mergeDoc).map(_.n))

        val exid = mergeDoc.getAs[Int]("expID")
        val expid = exid match { case None => "" case Some(str) => str }

        /*
        val driver = GraphDatabase.driver(neo4jUrl, AuthTokens.basic(userName, userPassword))
        val session = driver.session
        val script = s"CREATE (e:Experience { expID:${expid}, memberID:${ b.memberID }, current:'yes', position:'$positionVal', industry:'$industryVal', employer_name:'$employerVal', updated_date: TIMESTAMP() })  WITH e.MemberID AS memberid MATCH (u:Users {memberID: ${b.memberID} }), (exp:Experience {expID:${expid}})   CREATE(u)-[:WORKING_AT]->(exp)  RETURN exp "

        val res = session.run(script)
        session.close()
        driver.close()
        */
        
      } else if(employmentstatus == "2"){
        val mergedoc = BSONDocument (
          "expID" -> getNextSequence("expID"), 
          "memberID" -> b.memberID, 
          "current" -> b.current,
          "position" -> positionVal, 
          "industry" -> industryVal, 
          "employer" -> employerVal,
          "updated_date" -> BSONDateTime(System.currentTimeMillis)
        )
        experienceCollection.flatMap(_.insert(mergedoc).map(_.n))

        
      } else if(employmentstatus == "3"){
        val mergedoc = BSONDocument (
          "expID" -> getNextSequence("expID"), 
          "memberID" -> b.memberID, 
          "current" -> b.current,
          "position" -> positionVal, 
          "industry" -> industryVal, 
          "employer" -> employerVal,
          "updated_date" -> BSONDateTime(System.currentTimeMillis)
        )
        experienceCollection.flatMap(_.insert(mergedoc).map(_.n))
      } else if(employmentstatus == "4"){
        val mergedoc = BSONDocument (
          "expID" -> getNextSequence("expID"), 
          "memberID" -> b.memberID, 
          "current" -> b.current,
          "position" -> positionVal, 
          "industry" -> industryVal, 
          "employer" -> employerVal,
          "updated_date" -> BSONDateTime(System.currentTimeMillis)
        )
        experienceCollection.flatMap(_.insert(mergedoc).map(_.n))
      } else if(employmentstatus == "5"){
        val mergedoc = BSONDocument (
          "expID" -> getNextSequence("expID"), 
          "memberID" -> b.memberID, 
          "position" -> positionVal, 
          "current" -> b.current,
          "industry" -> industryVal, 
          "employer" -> employerVal,
          "updated_date" -> BSONDateTime(System.currentTimeMillis)
        )
        experienceCollection.flatMap(_.insert(mergedoc).map(_.n))
      } else{
        println("Education to update!!!")
        val mergedoc = BSONDocument (
          "eduID" -> getNextSequence("eduID"), 
          "memberID" -> b.memberID, 
          "degree" -> degreeVal, 
          "school_name" -> school_nameVal, 
          "updated_date" -> BSONDateTime(System.currentTimeMillis)
        )
        educationCollection.flatMap(_.insert(mergedoc).map(_.n))
      }

      true 
    }
    else false

    /**

    val f = userCollection.flatMap(_.find(document(
      "email" -> b.email
      )). // query builder
      cursor[User]().collect[List]()) // collect using the result cursor

    println(b.email)
          
    f.onComplete {
        case Success(result) => 
          result.foreach(
            user => {
              println(selector)
              println(userCollection.flatMap(_.update(selector, modifier).map(_.n)))
              println(user)
              true
            }
          )
        case Failure(e) => { 
          e.printStackTrace
          println("Error in updateRecord..")
        }
    }
    
    val result = Await.result(f, 5000 millis)

    false
    */

  }
  
  def updateInterest(in:Interest): Boolean = {
    
    val memberID = in.memberID
    val interest = in.interest

    val userdoc = BSONDocument( "$addToSet" -> BSONDocument( "interest"-> interest))

    // Update interest flag = 1
    val userdoc1 = BSONDocument( "$set" -> BSONDocument( "interest_flag"->1))
    
    val selector = BSONDocument("memberID" -> in.memberID)

    println(memberID)

    val f1 =  userCollection.flatMap(
      _.update(selector, userdoc1).map(_.n))
    
    val f = userCollection.flatMap(
      _.update(selector, userdoc).map(_.n))

    var ret:Int = 0
    f.map(n => {
        ret = n
      }
    )
    

    Await.result(f, 1000 millis)

    if(ret > 0) true else false

  }

  def emailVerification(memberID: Int): Boolean = {
  
    val userdoc = BSONDocument( "$set" -> BSONDocument( "email_verification_flag"-> 1))
    val selector = BSONDocument("memberID" -> memberID)
    
    println(memberID)
    
    val f = userCollection.flatMap(
      _.update(selector, userdoc).map(_.n))

    var ret:Int = 0
    f.map(n => {
        ret = n
      }
    )
    

    Await.result(f, 1000 millis)

    if(ret > 0) true else false

  }
  

  def deleteRecordOld(email: String): Int = {

    val f = userCollection.flatMap(_.remove(document(
      "email" -> email
      ))) // collect using the result cursor
          
    var i: Int = 0
    f.onComplete {
        case Success(result) => {
          println(result)
          i = result.n
        }
        case Failure(e) => { 
          e.printStackTrace
          println("Error in deleteRecord..")
        }
    }

    Await.result(f, 1000 millis);
    i    
  }

  def deleteRecord(field: String, value: String): Int = {

    val selector = if(value forall Character.isDigit) BSONDocument(field -> value.toInt) else BSONDocument(field -> value) 
    val f = userCollection.flatMap(_.remove(selector).map(_.n)) // collect using the result cursor

    /*
    var i: Int = 0
    f.onComplete {
        case Success(result) => {
          println(result)
          i = result.n
        }
        case Failure(e) => { 
          e.printStackTrace
          println("Error in deleteRecord..")
        }
    }
    */
    
    val i = Await.result(f, 5000 millis)

    println("Del count: " + i)

    i    
  }
  
  def retrieveJobtiile(position: String): MutableList[GetJobTitle] =  {
    
    val f = experienceCollection.flatMap(_.find(document("position" -> position)). // query builder
      cursor[Experience]().collect[List]()) // collect using the result cursor

    val records = MutableList[GetJobTitle]()

    f.onComplete {
        case Success(result) => 
          result.foreach(
            record => {
              val company: GetJobTitle = new GetJobTitle(
                record.position)
              records += company
            }
          )
        case Failure(e) => { 
          e.printStackTrace
          println("Error in retrieveJobtiile..")
        }
    }

    Await.result(f, 1000 millis);
    records

  }



  def friendRequest(myID: Int, friendID: Int): Int = {

    val driver = GraphDatabase.driver(neo4jUrl, AuthTokens.basic(userName, userPassword))
    val session = driver.session
    val script = s"MATCH (a:Members {memberID:'${myID}'} ), (b:Members {memberID:'${friendID}'} ) CREATE (a)-[r:FRIEND {status:'pending', conn_type:'school'}]->(b)   RETURN a "
    val result = session.run(script)

    session.close()
    driver.close()
    result.consume().counters().relationshipsCreated()
  }





  
}

object DatabaseAccess extends DatabaseAccess




