package io.cloudstate.kotlinsupport.initializers

import io.cloudstate.kotlinsupport.StatefulServiceDescriptor
import io.cloudstate.kotlinsupport.logger
import net.bytebuddy.agent.ByteBuddyAgent

class CloudStateInitializer {
    private val log = logger()

    internal val statefulServiceDescriptors: MutableList<StatefulServiceDescriptor> = mutableListOf<StatefulServiceDescriptor>()

    internal var configInit = ConfigIntializer()
    internal var crdtSourcedInit = CrdtEntityInitializer()
    internal var eventSourcedInit = EventSourcedEntityInitializer()

    init {
        log.debug("Initializing ByteBuddy Agent....")
        ByteBuddyAgent.install()
    }

    fun config(configInitializer: ConfigIntializer.() -> Unit) {
        configInit.configInitializer()
    }

    fun eventsourced(eventSourcedInitializer: EventSourcedEntityInitializer.() -> Unit) {
        eventSourcedInit.eventSourcedInitializer()

        // This cast prevent 'smart cast is impossible' error
        val entityServiceType: Class<*> = eventSourcedInit.entityService!!.java as Class<*>

        statefulServiceDescriptors.add(
                StatefulServiceDescriptor(
                        entityType = eventSourcedInit.type,
                        serviceClass = entityServiceType,
                        descriptor = eventSourcedInit.descriptor,
                        additionalDescriptors = eventSourcedInit.additionalDescriptors,
                        persistenceId = eventSourcedInit.persistenceId,
                        snapshotEvery = eventSourcedInit.snapshotEvery
                )
        )

    }

    fun crdt(crdtInitializer: CrdtEntityInitializer.() -> Unit) {
        crdtSourcedInit.crdtInitializer()

        // This cast prevent 'smart cast is impossible' error
        val entityServiceType: Class<*> = crdtSourcedInit.entityService!!.java as Class<*>

        statefulServiceDescriptors.add(
                StatefulServiceDescriptor(
                        entityType = crdtSourcedInit.type,
                        serviceClass = entityServiceType,
                        descriptor = crdtSourcedInit.descriptor,
                        additionalDescriptors = crdtSourcedInit.additionalDescriptors)
        )

    }

}
