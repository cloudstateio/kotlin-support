package io.cloudstate.kotlinsupport.api.eventsourced

import com.google.protobuf.Any
import com.google.protobuf.Descriptors
import io.cloudstate.javasupport.eventsourced.*
import io.cloudstate.javasupport.impl.AnySupport
import io.cloudstate.javasupport.impl.ResolvedEntityFactory
import io.cloudstate.javasupport.impl.ResolvedServiceMethod
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

    override fun create(context: EventSourcedContext?): EventSourcedEntityHandler = EntityHandler(context)

    override fun resolvedMethods(): Map<String, ResolvedServiceMethod<*, *>> = resolvedMethods

    class EntityHandler(context: EventSourcedContext?) : EventSourcedEntityHandler {

        override fun handleSnapshot(snapshot: Any?, context: SnapshotContext?) {
            TODO("Not yet implemented")
        }

        override fun handleEvent(event: Any?, context: EventContext?) {
            TODO("Not yet implemented")
        }

        override fun snapshot(context: SnapshotContext?): Optional<Any> {
            TODO("Not yet implemented")
        }

        override fun handleCommand(command: Any?, context: CommandContext?): Optional<Any> {
            TODO("Not yet implemented")
        }

    }
}