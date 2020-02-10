package io.cloudstate.kotlinsupport

import com.google.protobuf.Descriptors

data class StatefulServiceDescriptor(
        val entityService: Class<*>?,
        val descriptor: Descriptors.ServiceDescriptor?,
        val additionalDescriptors: Array<Descriptors.FileDescriptor>,
        val persistenceId: String?,
        val snapshotEvery: Int = 100) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as StatefulServiceDescriptor

        if (entityService != other.entityService) return false
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
        var result = entityService?.hashCode() ?: 0
        result = 31 * result + descriptor.hashCode()
        result = 31 * result + (additionalDescriptors?.contentHashCode() ?: 0)
        result = 31 * result + (persistenceId?.hashCode() ?: 0)
        result = 31 * result + (snapshotEvery ?: 0)
        return result
    }
}