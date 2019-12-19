package io.cloudstate.kotlinsupport.initializers.eventsourced

import com.google.protobuf.Descriptors
import io.cloudstate.kotlinsupport.EntityType
import io.cloudstate.kotlinsupport.initializers.Initializer
import io.cloudstate.kotlinsupport.services.StatefulService

class EventSourcedEntityInitializer: Initializer {

    val type: EntityType? = EntityType.EventSourced
    var entityService: StatefulService? = null
    var descriptor: Descriptors.ServiceDescriptor? = null
    var additionalDescriptors: Array<Descriptors.FileDescriptor>? = null
    var persistenceId: String? = null
    var snapshotEvery: Int = 0

    override fun getEntityType(): EntityType? = this!!.type
}