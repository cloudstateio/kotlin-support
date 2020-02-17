package io.cloudstate.kotlinsupport.initializers

import com.google.protobuf.Descriptors
import io.cloudstate.kotlinsupport.EntityType

class EventSourcedEntityInitializer: Initializer {

    val type: EntityType = EntityType.EventSourced
    var entityService: Class<*>? = null
    var descriptor: Descriptors.ServiceDescriptor? = null
    lateinit var additionalDescriptors: Array<Descriptors.FileDescriptor>
    var persistenceId: String? = null
    var snapshotEvery: Int = 0

    override fun getEntityType(): EntityType = this.type
}