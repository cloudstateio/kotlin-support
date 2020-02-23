package io.cloudstate.kotlinsupport

import com.google.protobuf.Descriptors
import io.cloudstate.kotlinsupport.api.eventsourced.EventSourcedBuilder
import io.cloudstate.kotlinsupport.transcoding.Transcoder

data class StatefulServiceDescriptor(
        val entityType: EntityType,
        val serviceClass: Class<*>?,
        var eventSourcedEntityBuilder: EventSourcedBuilder<Any>? = null,
        var transcoder: Transcoder? = null,
        val descriptor: Descriptors.ServiceDescriptor?,
        val additionalDescriptors: Array<Descriptors.FileDescriptor>,
        val persistenceId: String? = "",
        val snapshotEvery: Int = 100) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as StatefulServiceDescriptor

        if (entityType != other.entityType) return false
        if (serviceClass != other.serviceClass) return false
        if (eventSourcedEntityBuilder != other.eventSourcedEntityBuilder) return false
        if (transcoder != other.transcoder) return false
        if (descriptor != other.descriptor) return false
        if (!additionalDescriptors.contentEquals(other.additionalDescriptors)) return false
        if (persistenceId != other.persistenceId) return false
        if (snapshotEvery != other.snapshotEvery) return false

        return true
    }

    override fun hashCode(): Int {
        var result = entityType.hashCode()
        result = 31 * result + (serviceClass?.hashCode() ?: 0)
        result = 31 * result + (eventSourcedEntityBuilder?.hashCode() ?: 0)
        result = 31 * result + (transcoder?.hashCode() ?: 0)
        result = 31 * result + (descriptor?.hashCode() ?: 0)
        result = 31 * result + additionalDescriptors.contentHashCode()
        result = 31 * result + (persistenceId?.hashCode() ?: 0)
        result = 31 * result + snapshotEvery
        return result
    }


}