package io.cloudstate.kotlinsupport.api

import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

/**
 * Annotation used to indicate that the annotated parameter accepts an entity id.
 *
 *
 * This parameter may appear on handler methods and constructors for any class that provides behavior for stateful
 * service entity.
 *
 *
 * The type of the parameter must be [String].
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
annotation class EntityId
