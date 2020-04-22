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
import java.lang.reflect.Method
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
    override fun create(context: EventSourcedContext?): EventSourcedEntityHandler =
            EntityHandler(entityClass, context, anySupport, behavior)

    class EntityHandler(
            private val entityClass: Class<*>,
            private val context: EventSourcedContext?,
            private val anySupport: AnySupport,
            private val behavior: KotlinEventBehaviorReflection) : EventSourcedEntityHandler {

        private val reflectionHelper: ReflectionHelper = ReflectionHelper()
        private val entity = {
            context?.let { DelegatingEventSourcedContext(it) }?.let { entityConstructor(it) }
        }

        override fun handleEvent(anyEvent: Any?, eventContext: EventContext?) {
            val event = anySupport.decode(anyEvent)

            val handler = behavior.getCachedEventHandlerForClass(event.javaClass)
            when (handler.isPresent) {
                true -> {
                    val ctx = eventContext?.let { DelegatingEventContext(it) }
                    handler.get().invoke(entity, event, ctx)
                }
                false -> throw RuntimeException(
                    "No event handler found for event ${event.javaClass} on ${entity.javaClass.name}")
            }
        }

        override fun handleCommand(command: Any?, context: CommandContext?): Optional<Any>? {
            val commandHandlers = behavior.commandHandlers

            if (!commandHandlers.contains(context?.commandName())) {
                throw RuntimeException(
                        "No command handler found for command [${context?.commandName()}] on ${entity.javaClass.name}"
                )
            }

            return commandHandlers[context?.commandName().toString()]?.invoke(entity, command!!, context!!)
        }

        override fun handleSnapshot(snapshot: Any?, context: SnapshotContext?) {
            TODO("Not yet implemented")
        }

        override fun snapshot(context: SnapshotContext?): Optional<Any> {
            TODO("Not yet implemented")
        }

        private fun entityConstructor(context: EventSourcedEntityCreationContext): kotlin.Any {
            val constructors = entityClass.constructors
            if (constructors?.isNotEmpty()!! && constructors.size == 1) {
                return EntityConstructorInvoker(reflectionHelper.ensureAccessible(constructors[0]))
            }
            throw RuntimeException("Only a single constructor is allowed on event sourced entities: $entityClass")
        }

    }

    class EventHandlerInvoker(private val method: Method, private val reflectionHelper: ReflectionHelper){

        fun invoke(entity: kotlin.Any?, event: kotlin.Any?, ctx: DelegatingEventContext?) {

        }

    }

    class CommandHandlerInvoker(
            private val method: Method,
            private val serviceMethod: ResolvedServiceMethod<*, *>,
            private val reflectionHelper: ReflectionHelper){

        private val name = serviceMethod.method().fullName
        private val outputType = serviceMethod.method().outputType

        fun invoke(entityInstance: kotlin.Any?, command: Any, context: CommandContext): Optional<Any>  {
            val parameters = reflectionHelper.getParameters(method, command, context);
            val result = method.invoke(entityInstance, parameters)
            return handleResult(result, outputType)
        }

        private fun handleResult(result: kotlin.Any?, outputType: Descriptors.Descriptor): Optional<Any> {
            TODO("Not yet implemented")
        }

    }

    class SnapshotHandlerInvoker(private val method: Method, private val reflectionHelper: ReflectionHelper) {

    }

    class SnapshotInvoker(private val method: Method, private val reflectionHelper: ReflectionHelper) {

    }

    class DelegatingEventContext(private val delegate: EventContext) : EventContext, EventBehaviorContext {
        override fun entityId(): String = delegate.entityId()
        override fun become(vararg behaviors: kotlin.Any?) {
            TODO("Not yet implemented")
        }

        override fun serviceCallFactory(): ServiceCallFactory = delegate.serviceCallFactory()

        override fun sequenceNumber(): Long = delegate.sequenceNumber()

    }

    class DelegatingEventSourcedContext(private val delegate: EventSourcedContext) : EventSourcedEntityCreationContext {
        override fun entityId(): String = delegate.entityId()
        override fun become(vararg behaviors: kotlin.Any?) {
            TODO("Not yet implemented")
        }

        override fun serviceCallFactory(): ServiceCallFactory = delegate.serviceCallFactory()
    }
}