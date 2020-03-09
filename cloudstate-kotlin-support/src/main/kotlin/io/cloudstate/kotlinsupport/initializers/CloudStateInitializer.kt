package io.cloudstate.kotlinsupport.initializers

import io.cloudstate.kotlinsupport.StatefulServiceDescriptor
import io.cloudstate.kotlinsupport.transcoding.CrdtTranscoder
import io.cloudstate.kotlinsupport.transcoding.EventSourcedTranscoder

class CloudStateInitializer {

    internal val statefulServiceDescriptors: MutableList<StatefulServiceDescriptor> = mutableListOf<StatefulServiceDescriptor>()

    var host: String = "0.0.0.0"
    var port: Int = 8088
    var loglevel: String = "DEBUG"

    internal var crdtSourcedInit = CrdtEntityInitializer()
    internal var eventSourcedInit = EventSourcedEntityInitializer()

    fun registerEventSourcedEntity(eventSourcedInitializer: EventSourcedEntityInitializer.() -> Unit) {
        eventSourcedInit.eventSourcedInitializer()

        // This cast prevent 'smart cast is impossible' error
        val entityServiceType: Class<*> = eventSourcedInit.entityService!!.java as Class<*>

        statefulServiceDescriptors.add(
                StatefulServiceDescriptor(
                        entityType = eventSourcedInit.type,
                        serviceClass = entityServiceType,
                        transcoder = entityServiceType?.let { EventSourcedTranscoder(it) },
                        descriptor = eventSourcedInit.descriptor,
                        additionalDescriptors = eventSourcedInit.additionalDescriptors,
                        persistenceId = eventSourcedInit.persistenceId,
                        snapshotEvery = eventSourcedInit.snapshotEvery
                )
        )

    }

    fun registerCrdtEntity(crdtInitializer: CrdtEntityInitializer.() -> Unit) {
        crdtSourcedInit.crdtInitializer()

        // This cast prevent 'smart cast is impossible' error
        val entityServiceType: Class<*> = crdtSourcedInit.entityService!!.java as Class<*>

        statefulServiceDescriptors.add(
                StatefulServiceDescriptor(
                        entityType = crdtSourcedInit.type,
                        serviceClass = entityServiceType,
                        transcoder = entityServiceType?.let { CrdtTranscoder(it) },
                        descriptor = crdtSourcedInit.descriptor,
                        additionalDescriptors = crdtSourcedInit.additionalDescriptors)
        )

    }

}