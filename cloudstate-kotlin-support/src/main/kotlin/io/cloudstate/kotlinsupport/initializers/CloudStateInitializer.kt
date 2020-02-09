package io.cloudstate.kotlinsupport.initializers

import com.google.protobuf.Descriptors
import io.cloudstate.javasupport.CloudState
import io.cloudstate.javasupport.impl.AnySupport
import io.cloudstate.javasupport.impl.eventsourced.AnnotationBasedEventSourcedSupport
import io.cloudstate.kotlinsupport.initializers.crdt.CrdtEntityInitializer
import io.cloudstate.kotlinsupport.initializers.eventsourced.EventSourcedEntityInitializer

class CloudStateInitializer {

    private val engine = CloudState()
    private var prefer: AnySupport.Prefer = AnySupport.PREFER_JAVA()

    var typeUrlPrefix: String = AnySupport.DefaultTypeUrlPrefix()
    var serviceName: String? = null
    var serviceVersion: String? = null
    var host: String = "0.0.0.0"
    var port: Int = 8088
    var functionTimeout: Long = 10000

    internal var crdtSourcedInit = CrdtEntityInitializer()
    internal var eventSourcedInit = EventSourcedEntityInitializer()

    fun registerEventSourcedEntity(eventSourcedInitializer: EventSourcedEntityInitializer.() -> Unit) {
        eventSourcedInit.eventSourcedInitializer()

        val anySupport = eventSourcedInit.additionalDescriptors?.let { newAnySupport(it) }

        engine.registerEventSourcedEntity(
                AnnotationBasedEventSourcedSupport(eventSourcedInit.entityService, anySupport, eventSourcedInit.descriptor),
                eventSourcedInit.descriptor,
                eventSourcedInit.persistenceId,
                eventSourcedInit.snapshotEvery,
                *eventSourcedInit.additionalDescriptors)

    }

    fun getEngine(): CloudState = engine

    fun registerCrdtEntity(crdtInitializer: CrdtEntityInitializer.() -> Unit) {

    }

    private fun newAnySupport(descriptors: Array<Descriptors.FileDescriptor>): AnySupport? {
        return AnySupport(descriptors, this.javaClass.classLoader, typeUrlPrefix, prefer)
    }

}