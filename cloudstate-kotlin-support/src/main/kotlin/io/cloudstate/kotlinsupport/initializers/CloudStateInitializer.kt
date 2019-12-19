package io.cloudstate.kotlinsupport.initializers

import io.cloudstate.kotlinsupport.initializers.eventsourced.EventSourcedEntityInitializer

class CloudStateInitializer {
    private val services = mutableMapOf<String?, EntityFunction>()

    var serviceName: String? = null

    var serviceVersion: String? = null

    var host: String = "0.0.0.0"

    var port: Int = 8088

    var functionTimeout: Long = 10000

    internal var crdtSourcedInit = CrdtEntityInitializer()
    internal var eventSourcedInit = EventSourcedEntityInitializer()

    fun registerEventSourcedEntity(eventSourcedInitializer: EventSourcedEntityInitializer.() -> Unit) {
        eventSourcedInit.eventSourcedInitializer()
        eventSourcedInit?.entityService?.let { services.put(eventSourcedInit.descriptor?.fullName, EntityFunction(eventSourcedInit, eventSourcedInit.type?.typeStr, eventSourcedInit.descriptor?.fullName)) }
    }

    fun registerCrdtEntity(crdtInitializer: CrdtEntityInitializer.() -> Unit) {
        crdtSourcedInit.crdtInitializer()
        crdtSourcedInit?.statefulService?.let { services.put(crdtSourcedInit.descriptor?.fullName, EntityFunction(crdtSourcedInit, crdtSourcedInit.type?.typeStr, crdtSourcedInit.descriptor?.fullName)) }
    }

    fun getServices(): Map<String?, EntityFunction> {
        return services
    }

    data class EntityFunction(val initializer: Initializer, val entityType: String?, val entityName: String?)

}