package io.cloudstate.samples

import akka.actor.ActorSystem
import akka.grpc.GrpcClientSettings
import akka.stream.ActorMaterializer
import com.example.sm.sm._

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

/**
 * Designed for use in the REPL, run sbt console and then new io.cloudstate.com.example.ShoppingCartClient("localhost", 9000)
 * @param hostname
 * @param port
 */
class SMClient(hostname: String, port: Int, hostnameOverride: Option[String], sys: ActorSystem) {
  def this(hostname: String, port: Int, hostnameOverride: Option[String] = None) =
    this(hostname, port, hostnameOverride, ActorSystem())
  private implicit val system = sys
  private implicit val materializer = ActorMaterializer()
  import sys.dispatcher

  val settings = {
    val s = GrpcClientSettings.connectToServiceAt(hostname, port).withTls(false)
    hostnameOverride.fold(s)(host => s.withChannelBuilderOverrides(_.overrideAuthority(host)))
  }
  println(s"Connecting to $hostname:$port")
  val service = com.example.sm.sm.SMClient(settings)

  def shutdown(): Unit = {
    await(service.close())
    await(system.terminate())
  }

  def await[T](future: Future[T]): T = Await.result(future, 10.seconds)

  def getHome(accountID: String) =
    await(service.getHome(GetHomeInfo(accountID)))
  def setMaxSession(accountID: String, max: Int) =
    await(service.setMaxSession(MaxSession(accountID, max)))
  def createSession(accountID: String, deviceId: String): SessionResponse =
    await(service.createSession(SessionSetup(accountID, deviceId)))
  def heartBeat(accountID: String, sessionID: String): SessionResponse =
    await(service.heartBeat(HeartBeatSession(accountID, sessionID)))
  def tearDown(accountID: String, sessionID: String) =
    await(service.tearDown(TearDownSession(accountID, sessionID)))

}
