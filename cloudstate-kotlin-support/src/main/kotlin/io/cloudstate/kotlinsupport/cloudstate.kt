package io.cloudstate.kotlinsupport

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import io.cloudstate.kotlinsupport.initializers.CloudStateInitializer
import java.lang.IllegalArgumentException

fun cloudstate(paramsInitializer: CloudStateInitializer.() -> Unit): CloudStateRunner {

    val cloudStateInitializer = CloudStateInitializer()
    cloudStateInitializer.paramsInitializer()

    val type = cloudStateInitializer.eventSourcedInit.type
            ?: throw IllegalArgumentException("type must be set")

    val persistenceId = cloudStateInitializer.eventSourcedInit.persistenceId
            ?: throw IllegalArgumentException("persistenceId must be set")

    // important to enable HTTP/2 in ActorSystem's config
    val conf = getConfig()

    var system = getActorSystem(conf)
    var materializer = ActorMaterializer.create(system)

    val services = cloudStateInitializer.getServices()

    if (services.isEmpty()){
        throw IllegalStateException("StatefulService must be set")
    }

    return CloudStateRunner(cloudStateInitializer, services as java.util.HashMap, system, materializer)
}

private fun getConfig() =
        ConfigFactory.parseString("akka.http.server.preview.enable-http2 = on")
        .withFallback(ConfigFactory.defaultApplication()).resolve()

private fun getActorSystem(conf: Config) =
        ActorSystem.create("CloudState", conf)

