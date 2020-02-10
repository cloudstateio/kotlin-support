package io.cloudstate.kotlinsupport.initializers

import io.cloudstate.kotlinsupport.EntityType

interface Initializer {
    fun getEntityType(): EntityType
}
