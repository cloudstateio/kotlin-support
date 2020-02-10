package io.cloudstate.kotlinsupport.initializers.crdt

import com.google.protobuf.Descriptors
import io.cloudstate.kotlinsupport.EntityType
import io.cloudstate.kotlinsupport.initializers.Initializer

class CrdtEntityInitializer: Initializer {

    val type: EntityType = EntityType.Crdt
    var descriptor: Descriptors.ServiceDescriptor? = null
    lateinit var additionalDescriptors: Array<Descriptors.FileDescriptor>

    override fun getEntityType(): EntityType = this.type

}
