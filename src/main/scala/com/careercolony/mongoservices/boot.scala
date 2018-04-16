package com.careercolony.mongoservices

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import com.careercolony.mongoservices.routes.UserService

import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model.{StatusCodes, HttpResponse}
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{Directive0, Route}
import com.typesafe.config.ConfigFactory


class StartNeo4jServer(implicit val system: ActorSystem,
                       implicit val materializer: ActorMaterializer) extends UserService {
  

  def startServer(address: String, port: Int) = {
   val port = if (sys.env.contains("PORT")) sys.env("PORT").toInt else config.getInt("http.port")
    Http().bindAndHandle(userRoutes, address, port)
  }
}

object StartApplication extends App {
  StartApp
}

object StartApp {
  implicit val system: ActorSystem = ActorSystem("Neo4j-Akka-Service")
  implicit val executor = system.dispatcher
  implicit val materializer = ActorMaterializer()
  val server = new StartNeo4jServer()
  val config = server.config
  val serverUrl = config.getString("http.interface")
  val port = config.getInt("http.port")
  server.startServer(serverUrl, port)
}