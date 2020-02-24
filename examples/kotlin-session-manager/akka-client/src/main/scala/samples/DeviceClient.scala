package io.cloudstate.samples

import akka.actor.ActorSystem
import akka.grpc.GrpcClientSettings
import akka.stream.ActorMaterializer
import com.example.smdevice.smdevice._

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

/**
 * Designed for use in the REPL, run sbt console and then new io.cloudstate.com.example.ShoppingCartClient("localhost", 9000)
 *
 * @param hostname
 * @param port
 */
class DeviceClient(hostname: String, port: Int, hostnameOverride: Option[String], sys: ActorSystem) {
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
  val service = com.example.smdevice.smdevice.DeviceClient(settings)

  def shutdown(): Unit = {
    await(service.close())
    await(system.terminate())
  }

  def await[T](future: Future[T]): T = Await.result(future, 10.seconds)

  def getDevice(deviceID: String) = await(service.getDevice(GetDeviceInfo(deviceID)))
  def createSessionWithDevice(deviceId: String) =
    await(service.createSessionWithDevice(SessionSetupWithDevice(deviceId)))

  def createDevice(deviceId: String, accountID: String) =
    await(service.createDevice(CreateDeviceParam(deviceId, accountID)))
  def deleteDevice(deviceId: String, accountID: String) =
    await(service.deleteDevice(DeleteDeviceParam(deviceId)))

}
