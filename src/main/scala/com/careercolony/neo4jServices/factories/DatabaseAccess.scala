package com.careercolony.neo4jServices.factories

import org.neo4j.driver.v1._
import com.typesafe.config.ConfigFactory
import java.security.MessageDigest

import scala.collection.mutable.MutableList;





case class User2(memberID: Int, firstname: String, lastname: String, email: String)
case class User3(memberID: Int, firstname: String, lastname: String, email: String, interest: String, employmentstatus: String, avatar: String)
case class Credentials(email: String, password: String)
case class User(firstname: String, lastname: String, email: String, password: String)
case class Wizard(email: String, current_employment_status: String, interest: String, current_job_title: String, current_employer: String)
case class ListUser(companies: MutableList[User2])
case class ResponseStatus(status: Int, message: String, details: String)
case class GetJobTitle(position: String)
case class BioData(memberID: Int, userIP: Option[String], email:String, country:String, interest:String, employmentstatus:String, employer_name:Option[String], position:Option[String], industry:Option[String], degree:Option[String], school_name:Option[String])
case class Experience(memberID: String, employer: String, position: String)

trait DatabaseAccess {
  

  val config = ConfigFactory.load("application.conf")
  val neo4jUrl = config.getString("neo4j.url")
  val userName = config.getString("neo4j.userName")

  val userPassword = config.getString("neo4j.userPassword")

  def md5(s: String) = {
    MessageDigest.getInstance("MD5").digest(s.getBytes)
  }

  

  def login(l: Credentials) = {
    
    val driver = GraphDatabase.driver(neo4jUrl, AuthTokens.basic(userName, userPassword))
    val session = driver.session
    val script = s"OPTIONAL MATCH (a:Members {email:'${ l.email}',  password:'${ l.password}'}) RETURN a.memberID AS memberID, a.firstname AS firstname, a.lastname AS lastname, a.email AS email"
    val result: StatementResult = session.run(script)
    val records = MutableList[User2]()
   if(result.hasNext()){
        while (result.hasNext()) {
          val record = result.next()
          val user: User2 = new User2(record.get("memberID").asInt, record.get("firstname").asString(), record.get("lastname").asString(), record.get("email").asString())
          
          records += user
        } 

        session.close()
        driver.close()
        records
    }else{
        println("User does not exist");
      
         session.close()
         driver.close()
         records
    }
  }

  def insertRecord(s: User): MutableList[User2] = {
    val driver = GraphDatabase.driver(neo4jUrl, AuthTokens.basic(userName, userPassword))
    val session = driver.session
    val script = s"MATCH (a:Members {email:'${ s.email}'})  RETURN a.email AS email"
    val result: StatementResult = session.run(script)
    val records = MutableList[User2]()
     // Check if user exist
     if(result.hasNext()){
         println("User already exist");
  
         session.close()
         driver.close()
         records
      } else {
         val script = s"MERGE (id:UniqueId{name:'Members'}) ON CREATE SET id.count = 1 ON MATCH SET id.count = id.count + 1 WITH toInt(id.count) AS memid CREATE (s:Members {memberID:memid, firstname:'${ s.firstname }', lastname:'${ s.lastname }', email:'${ s.email }', avatar:'avatar.png', password:'${s.password}', signup_date: TIMESTAMP() }) WITH memid AS id MATCH (m:Members {memberID:id}) RETURN m.memberID AS memberID, m.firstname AS firstname, m.lastname AS lastname, m.email AS email"
         val result: StatementResult = session.run(script)
         val records = MutableList[User2]()
   
          while (result.hasNext()) {
            val record = result.next()
            val user: User2 = new User2(record.get("memberID").asInt, record.get("firstname").asString(), record.get("lastname").asString(), record.get("email").asString())
            
            records += user
          } 

          session.close()
          driver.close()
          records
      }
  }

  def retrieveRecord(memberID: Int): MutableList[User3] = {
    val driver = GraphDatabase.driver(neo4jUrl, AuthTokens.basic(userName, userPassword))
    val session = driver.session
    val script = s"MATCH (a:Members) WHERE a.memberID = $memberID RETURN a.memberID AS memberID, a.firstname AS firstname, a.lastname AS lastname, a.interest AS interest, a.avatar AS avatar, a.email AS " +
                 s"email"
    val result = session.run(script)
    val records = MutableList[User3]()

    while (result.hasNext()) {
        val record = result.next()
        val user: User3 = new User3(record.get("memberID").asInt, record.get("firstname").asString(), record.get("lastname").asString(), record.get("email").asString(), record.get("interest").asString(), record.get("employmentstatus").asString(), record.get("avatar").asString())
        
        records += user
      } 

      session.close()
      driver.close()
      records
  }

  def retrieveExperience(memberID: String): MutableList[Experience] = {
    val driver = GraphDatabase.driver(neo4jUrl, AuthTokens.basic(userName, userPassword))
    val session = driver.session
    val script = s"MATCH (a:Experience) WHERE a.memberID = '$memberID' AND a.current = 'yes' RETURN a.memberID AS memberID, a.employer_name AS employer, a.position AS " +
                 s"position"
    val result = session.run(script)
    val records = MutableList[Experience]()

    while (result.hasNext()) {
        val record = result.next()
        val exp: Experience = new Experience(record.get("memberID").asString, record.get("employer").asString(), record.get("position").asString())
        
        records += exp
      } 

      session.close()
      driver.close()
      records
  }
  

  def retrieveRecords() =  {
    val driver = GraphDatabase.driver(neo4jUrl, AuthTokens.basic(userName, userPassword))
    val session = driver.session
    //val script = s"MATCH (a:Members) RETURN a.firstname AS firstname, a.email AS " +s"email"
   
   val script = s"MATCH (n:Members) RETURN n{.*} AS members LIMIT 25"
   val result: StatementResult = session.run(script)

   // Neo4J 3.1 you can use Map projections       
    val record_data = if (result.hasNext()) {
      val e = result.next()
      //val res = result.list()
      println(e.get("members"))
      val results = e.get("members")
      Some(results)
      } else {
      None
      }

      session.close()
      driver.close()
      record_data
    }

  def updateRecord(email: String, newName: String): Boolean = {
    val driver = GraphDatabase.driver(neo4jUrl, AuthTokens.basic(userName, userPassword))
    val session = driver.session
    val script =
      s"MATCH (user:Members) where user.email ='$email' SET user.firstname = '$newName' RETURN user.firstname" +
      s" AS firstname, user.email AS email"
    val result = session.run(script)
    session.close()
    driver.close()
    result.consume().counters().containsUpdates()
  }

  def updatesteps(b:BioData): Boolean = {
    val driver = GraphDatabase.driver(neo4jUrl, AuthTokens.basic(userName, userPassword))
    val session = driver.session
    val employmentstatus = b.employmentstatus

    println(employmentstatus)
   
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
    }else if(employmentstatus == "2"){
      val script = s"MATCH (s:Members) where s.email ='${b.email}' SET s.country = '${b.country}', s.userIP = '$useripVal' , s.interest ='${b.interest}', s.employmentstatus= ${b.employmentstatus} MERGE (id:UniqueId{name:'Experience'}) ON CREATE SET id.count = 1 ON MATCH SET id.count = id.count + 1 WITH toInt(id.count) AS expid CREATE (b:Experience { expID:expid, memberID:'${ b.memberID }', current:'yes', position:'$positionVal', industry:'$industryVal', employer_name:'$employer_nameVal', updated_date: TIMESTAMP() })  RETURN b.memberID" +
        s" AS memberID"
      val result = session.run(script)
      session.close()
      driver.close()
      result.consume().counters().containsUpdates()
    }else if(employmentstatus == "3"){
      val script = s"MATCH (s:Members) where s.email ='${b.email}' SET s.country = '${b.country}', s.userIP = '$useripVal' , s.interest ='${b.interest}', s.employmentstatus= ${b.employmentstatus} MERGE (id:UniqueId{name:'Experience'}) ON CREATE SET id.count = 1 ON MATCH SET id.count = id.count + 1 WITH toInt(id.count) AS expid CREATE (b:Experience { expID:expid, memberID:'${ b.memberID }', current:'yes', position:'$positionVal', industry:'$industryVal', employer_name:'$employer_nameVal', updated_date: TIMESTAMP() })  RETURN b.memberID" +
        s" AS memberID"
      val result = session.run(script)
      session.close()
      driver.close()
      result.consume().counters().containsUpdates()
    }else if(employmentstatus == "4"){
      val script = s"MATCH (s:Members) where s.email ='${b.email}' SET s.country = '${b.country}', s.userIP = '$useripVal' , s.interest ='${b.interest}', s.employmentstatus= ${b.employmentstatus} MERGE (id:UniqueId{name:'Experience'}) ON CREATE SET id.count = 1 ON MATCH SET id.count = id.count + 1 WITH toInt(id.count) AS expid CREATE (b:Experience { expID:expid, memberID:'${ b.memberID }', current:'yes', position:'$positionVal', industry:'$industryVal', employer_name:'$employer_nameVal', updated_date: TIMESTAMP() })  RETURN b.memberID" +
        s" AS memberID"
      val result = session.run(script)
      session.close()
      driver.close()
      result.consume().counters().containsUpdates()
    }else if(employmentstatus == "5"){
      val script = s"MATCH (s:Members) where s.email ='${b.email}' SET s.country = '${b.country}', s.userIP = '$useripVal' , s.interest ='${b.interest}', s.employmentstatus= ${b.employmentstatus} MERGE (id:UniqueId{name:'Experience'}) ON CREATE SET id.count = 1 ON MATCH SET id.count = id.count + 1 WITH toInt(id.count) AS expid CREATE (b:Experience { expID:expid, memberID:'${ b.memberID }', position:'$positionVal', industry:'$industryVal', employer_name:'$employer_nameVal', updated_date: TIMESTAMP() })  RETURN b.memberID" +
        s" AS memberID"
      val result = session.run(script)
      session.close()
      driver.close()
      result.consume().counters().containsUpdates()
    }else{
      val script = s"MATCH (s:Members) where s.email ='${b.email}' SET s.country = '${b.country}', s.userIP = '$useripVal' , s.interest ='${b.interest}', s.employmentstatus= ${b.employmentstatus} MERGE (id:UniqueId{name:'Education'}) ON CREATE SET id.count = 1 ON MATCH SET id.count = id.count + 1 WITH toInt(id.count) AS eduid CREATE (b:Education { eduID:eduid, memberID:'${ b.memberID }', degree:'$degreeVal', school_name:'$school_nameVal', updated_date: TIMESTAMP() })  RETURN b.memberID" +
        s" AS memberID"
      val result = session.run(script)
      session.close()
      driver.close()
      result.consume().counters().containsUpdates()
    }

  }
  

  def deleteRecord(email: String): Int = {
    val driver = GraphDatabase.driver(neo4jUrl, AuthTokens.basic(userName, userPassword))
    val session = driver.session
    val script = s"MATCH (user:Members) where user.email ='$email' Delete user"
    val result = session.run(script)
    session.close()
    driver.close()
    result.consume().counters().nodesDeleted()
  }

  def retrieveJobtiile(position: String): MutableList[GetJobTitle] =  {
    val driver = GraphDatabase.driver(neo4jUrl, AuthTokens.basic(userName, userPassword))
    val session = driver.session

   val script = s"MATCH (a:Experience) WHERE a.position =~'(?i).*$position.*'  RETURN  a.position AS position"
   val result: StatementResult = session.run(script)
      
   val records = MutableList[GetJobTitle]()
     while (result.hasNext()) {
        
          val record = result.next()
      
          val company: GetJobTitle = new GetJobTitle(record.get("position").asString())
         
          records += company
     } 

    session.close()
    driver.close()
    
    records

    }
  def createNodesWithRelation(email: String, other_member_email:String) = {
    val driver = GraphDatabase.driver(neo4jUrl, AuthTokens.basic(userName, userPassword))
    val session = driver.session
    //val nameOfFriends = "\"" + userList.mkString("\", \"") + "\""
    //val script = s"MATCH (user:Users {firstname: '$user_name'}) FOREACH (firstname in [$nameOfFriends] | " +
                 //s"CREATE (user)-[:$relation]->(:Users {firstname:name}))"

    val script = s"MATCH(a:Members  {email:'${email}'}), (b:Members {email:'${other_member_email}'}) CREATE(a)-[:FRIENDS {status:'active', date:'2017-09-07'}]->(b) "
    val result = session.run(script)
    session.close()
    driver.close()
    result.consume().counters().relationshipsCreated()
  }

  
}

object DatabaseAccess extends DatabaseAccess

