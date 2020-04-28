package io.cloudstate.kotlinsupport.initializers

import com.google.protobuf.Descriptors
import io.cloudstate.kotlinsupport.EntityType
import kotlin.reflect.KClass

class CrdtEntityInitializer: Initializer {
    val type: EntityType = EntityType.Crdt
    var entityService: KClass<*>? = null
    var descriptor: Descriptors.ServiceDescriptor? = null
    var additionalDescriptors: List<Descriptors.FileDescriptor> = mutableListOf()

    override fun getEntityType(): EntityType = this.type

}
