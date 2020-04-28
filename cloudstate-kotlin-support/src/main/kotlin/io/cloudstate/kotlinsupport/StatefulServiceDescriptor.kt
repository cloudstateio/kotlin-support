package io.cloudstate.kotlinsupport

import com.google.protobuf.Descriptors
import io.cloudstate.kotlinsupport.api.transcoding.Transcoder
import kotlin.reflect.KClass

data class StatefulServiceDescriptor(
        val entityType: EntityType,
        val serviceClass: KClass<*>?,
        var transcoder: Transcoder? = null,
        val descriptor: Descriptors.ServiceDescriptor?,
        val additionalDescriptors: List<Descriptors.FileDescriptor> = mutableListOf(),
        val persistenceId: String? = "",
        val snapshotEvery: Int = 100)
