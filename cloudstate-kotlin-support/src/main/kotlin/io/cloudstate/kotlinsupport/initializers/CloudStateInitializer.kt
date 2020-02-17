package io.cloudstate.kotlinsupport.initializers

import io.cloudstate.kotlinsupport.StatefulServiceDescriptor
import io.cloudstate.kotlinsupport.initializers.crdt.CrdtEntityInitializer
import io.cloudstate.kotlinsupport.initializers.eventsourced.EventSourcedEntityInitializer

class CloudStateInitializer {

    internal val statefulServiceDescriptors: MutableList<StatefulServiceDescriptor> = mutableListOf<StatefulServiceDescriptor>()

    var host: String = "0.0.0.0"
    var port: Int = 8088
    var loglevel: String = "DEBUG"

    internal var crdtSourcedInit = CrdtEntityInitializer()
    internal var eventSourcedInit = EventSourcedEntityInitializer()

    fun registerEventSourcedEntity(eventSourcedInitializer: EventSourcedEntityInitializer.() -> Unit) {
        eventSourcedInit.eventSourcedInitializer()

        statefulServiceDescriptors.add(
                StatefulServiceDescriptor(
                        entityType = eventSourcedInit.type,
                        entityService = eventSourcedInit.entityService,
                        descriptor = eventSourcedInit.descriptor,
                        additionalDescriptors = eventSourcedInit.additionalDescriptors,
                        persistenceId = eventSourcedInit.persistenceId,
                        snapshotEvery = eventSourcedInit.snapshotEvery
                )
        )

    }

    fun registerCrdtEntity(crdtInitializer: CrdtEntityInitializer.() -> Unit) {
        crdtSourcedInit.crdtInitializer()

        statefulServiceDescriptors.add(
                StatefulServiceDescriptor(
                        entityType = crdtSourcedInit.type,
                        descriptor = crdtSourcedInit.descriptor,
                        additionalDescriptors = crdtSourcedInit.additionalDescriptors)

        )

    }

}