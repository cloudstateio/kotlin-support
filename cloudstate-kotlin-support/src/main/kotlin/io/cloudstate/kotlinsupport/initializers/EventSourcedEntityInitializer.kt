package io.cloudstate.kotlinsupport.initializers

import com.google.protobuf.Descriptors
import io.cloudstate.kotlinsupport.EntityType
import kotlin.reflect.KClass

class EventSourcedEntityInitializer: Initializer {

    val type: EntityType = EntityType.EventSourced
    var entityService: KClass<*>? = null
    var descriptor: Descriptors.ServiceDescriptor? = null
    lateinit var additionalDescriptors: List<Descriptors.FileDescriptor>
    var persistenceId: String? = null
    var snapshotEvery: Int = 0

    override fun getEntityType(): EntityType = this.type
}