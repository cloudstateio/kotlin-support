package io.cloudstate.kotlinsupport.initializers.crdt

import com.google.protobuf.Descriptors
import io.cloudstate.kotlinsupport.EntityType
import io.cloudstate.kotlinsupport.initializers.Initializer

class CrdtEntityInitializer: Initializer {

    val type: EntityType? = EntityType.EventSourced
    var descriptor: Descriptors.ServiceDescriptor? = null
    var additionalDescriptors: Array<Descriptors.FileDescriptor>? = null
    var persistenceId: String? = null

    override fun getEntityType(): EntityType? = this!!.type

}
