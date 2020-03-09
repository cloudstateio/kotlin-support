package io.cloudstate.kotlinsupport

import com.google.protobuf.Descriptors
import io.cloudstate.kotlinsupport.transcoding.Transcoder

data class StatefulServiceDescriptor(
        val entityType: EntityType,
        val serviceClass: Class<*>?,
        var transcoder: Transcoder? = null,
        val descriptor: Descriptors.ServiceDescriptor?,
        val additionalDescriptors: Array<Descriptors.FileDescriptor> = arrayOf(),
        val persistenceId: String? = "",
        val snapshotEvery: Int = 100) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as StatefulServiceDescriptor

        if (transcoder != other.transcoder) return false
        if (descriptor != other.descriptor) return false
        if (additionalDescriptors != null) {
            if (other.additionalDescriptors == null) return false
            if (!additionalDescriptors.contentEquals(other.additionalDescriptors)) return false
        } else if (other.additionalDescriptors != null) return false
        if (persistenceId != other.persistenceId) return false
        if (snapshotEvery != other.snapshotEvery) return false

        return true
    }

    override fun hashCode(): Int {
        var result = transcoder?.hashCode() ?: 0
        result = 31 * result + descriptor.hashCode()
        result = 31 * result + (additionalDescriptors?.contentHashCode() ?: 0)
        result = 31 * result + (persistenceId?.hashCode() ?: 0)
        result = 31 * result + (snapshotEvery ?: 0)
        return result
    }
}