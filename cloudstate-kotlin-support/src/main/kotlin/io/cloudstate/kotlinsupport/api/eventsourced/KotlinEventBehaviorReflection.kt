package io.cloudstate.kotlinsupport.api.eventsourced

import io.cloudstate.javasupport.impl.ResolvedServiceMethod
import io.cloudstate.kotlinsupport.ReflectionHelper
import scala.collection.immutable.Map

class KotlinEventBehaviorReflection(entityClass: Class<*>, resolvedMethods: Map<String, ResolvedServiceMethod<*, *>>) {
    private val reflectionHelper: ReflectionHelper = ReflectionHelper()
    val allMethods = reflectionHelper.getAllDeclaredMethods(entityClass)
}