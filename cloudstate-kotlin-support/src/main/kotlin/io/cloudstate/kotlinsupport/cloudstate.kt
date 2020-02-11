package io.cloudstate.kotlinsupport

import io.cloudstate.kotlinsupport.initializers.CloudStateInitializer
import java.lang.IllegalArgumentException

fun cloudstate(paramsInitializer: CloudStateInitializer.() -> Unit): CloudStateRunner {

    val cloudStateInitializer = CloudStateInitializer()
    cloudStateInitializer.paramsInitializer()

    val type = cloudStateInitializer.eventSourcedInit.type
            ?: throw IllegalArgumentException("type must be set")

    val persistenceId = cloudStateInitializer.eventSourcedInit.persistenceId
            ?: throw IllegalArgumentException("persistenceId must be set")

    return CloudStateRunner(cloudStateInitializer)
            .withAllRegisters()
}

