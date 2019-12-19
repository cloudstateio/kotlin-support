package io.cloudstate.kotlinsupport.initializers

import com.google.protobuf.Descriptors
import io.cloudstate.kotlinsupport.EntityType
import io.cloudstate.kotlinsupport.services.StatefulService

class CrdtEntityInitializer: Initializer {

    val type: EntityType? = EntityType.Crdt
    var statefulService: StatefulService? = null
    var descriptor: Descriptors.ServiceDescriptor? = null
    var additionalDescriptors: Array<Descriptors.FileDescriptor>? = null
    var persistenceId: String? = null

    override fun getEntityType(): EntityType? = type

}
