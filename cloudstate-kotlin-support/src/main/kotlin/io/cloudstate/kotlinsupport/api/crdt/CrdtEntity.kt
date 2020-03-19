package io.cloudstate.kotlinsupport.api.crdt

import io.cloudstate.javasupport.impl.CloudStateAnnotation
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

/**
 * A CRDT backed entity.
 *
 *
 * CRDT entities store their state in a subclass [Crdt]. These may be created using a
 * [CrdtFactory], which can be injected into the constructor or as a parameter to any [ ] annotated method.
 *
 *
 * Only one CRDT may be created, it is important that before creating a CRDT, the entity should
 * check whether the CRDT has already been created, for example, it may have been created on another
 * node and replicated to this node. To check, either use the [CrdtContext.state]
 * method, which can be injected into the constructor or any [CommandHandler] method, or have
 * an instance of the CRDT wrapped in [java.util.Optional] injected into the constructor or
 * command handler methods.
 */
@CloudStateAnnotation
@Target(AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.CLASS)
@Retention(RetentionPolicy.RUNTIME)
annotation class CrdtEntity