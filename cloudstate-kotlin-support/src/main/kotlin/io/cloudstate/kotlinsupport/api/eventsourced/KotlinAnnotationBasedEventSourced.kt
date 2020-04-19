package io.cloudstate.kotlinsupport.api.eventsourced

import com.google.protobuf.Any
import com.google.protobuf.Descriptors
import io.cloudstate.javasupport.ServiceCallFactory
import io.cloudstate.javasupport.eventsourced.*
import io.cloudstate.javasupport.impl.AnySupport
import io.cloudstate.javasupport.impl.ResolvedEntityFactory
import io.cloudstate.javasupport.impl.ResolvedServiceMethod
import io.cloudstate.javasupport.impl.eventsourced.EntityConstructorInvoker
import io.cloudstate.kotlinsupport.ReflectionHelper
import scala.collection.immutable.Map
import java.util.*

class KotlinAnnotationBasedEventSourced(
        private val entityClass: Class<*>,
        private val anySupport: AnySupport,
        private val resolvedMethods: Map<String, ResolvedServiceMethod<*, *>>)
    : EventSourcedEntityFactory, ResolvedEntityFactory {

    constructor(entityClass: Class<*>,
                anySupport: AnySupport,
                serviceDescriptor: Descriptors.ServiceDescriptor)
            : this(entityClass, anySupport, anySupport.resolveServiceDescriptor(serviceDescriptor))

    private val behavior = KotlinEventBehaviorReflection(entityClass, resolvedMethods)

    override fun resolvedMethods(): Map<String, ResolvedServiceMethod<*, *>> = resolvedMethods
    override fun create(context: EventSourcedContext?): EventSourcedEntityHandler = EntityHandler(entityClass, context, anySupport)

    class EntityHandler(
            private val entityClass: Class<*>,
            private val context: EventSourcedContext?,
            private val anySupport: AnySupport) : EventSourcedEntityHandler {

        private val reflectionHelper: ReflectionHelper = ReflectionHelper()
        private val entity = {
            context?.let { DelegatingEventSourcedContext(it) }?.let { constructor(it) }
        }

        override fun handleSnapshot(snapshot: Any?, context: SnapshotContext?) {
            TODO("Not yet implemented")
        }

        override fun handleEvent(anyEvent: Any?, context: EventContext?) {
            val event = anySupport.decode(anyEvent)
        }

        override fun snapshot(context: SnapshotContext?): Optional<Any> {
            TODO("Not yet implemented")
        }

        override fun handleCommand(command: Any?, context: CommandContext?): Optional<Any> {
            TODO("Not yet implemented")
        }

        private fun constructor(context: EventSourcedEntityCreationContext): kotlin.Any {
            val constructors = entityClass.constructors

            if (constructors?.isNotEmpty()!! && constructors.size == 1) {
                return EntityConstructorInvoker(reflectionHelper.ensureAccessible(constructors[0]))
            }

            throw RuntimeException("Only a single constructor is allowed on event sourced entities: $entityClass")
        }

    }

    class DelegatingEventSourcedContext(private val delegate: EventSourcedContext) : EventSourcedEntityCreationContext {
        override fun entityId(): String = delegate.entityId()
        override fun become(vararg behaviors: kotlin.Any?) {
            TODO("Not yet implemented")
        }

        override fun serviceCallFactory(): ServiceCallFactory = delegate.serviceCallFactory()
    }
}