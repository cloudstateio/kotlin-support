package io.cloudstate.kotlinsupport.api.eventsourced

import com.google.protobuf.Any
import com.google.protobuf.Descriptors
import com.google.protobuf.Message
import io.cloudstate.javasupport.ServiceCallFactory
import io.cloudstate.javasupport.eventsourced.*
import io.cloudstate.javasupport.impl.AnySupport
import io.cloudstate.javasupport.impl.ResolvedEntityFactory
import io.cloudstate.javasupport.impl.ResolvedServiceMethod
import io.cloudstate.kotlinsupport.ReflectionHelper
import io.cloudstate.kotlinsupport.annotations.EntityId
import io.cloudstate.kotlinsupport.logger
import scala.collection.immutable.Map
import java.lang.reflect.Constructor
import java.lang.reflect.Method
import java.lang.reflect.Parameter
import java.util.*
import kotlin.reflect.KClass

class KotlinAnnotationBasedEventSourced(
        private val entityClass: KClass<*>,
        private val anySupport: AnySupport,
        private val resolvedMethods: Map<String, ResolvedServiceMethod<*, *>>)
    : EventSourcedEntityFactory, ResolvedEntityFactory {

    private val log = logger()
    private val behavior = KotlinEventBehaviorReflection(entityClass, resolvedMethods, anySupport)

    constructor(entityClass: KClass<*>,
                anySupport: AnySupport,
                serviceDescriptor: Descriptors.ServiceDescriptor)
            : this(entityClass, anySupport, anySupport.resolveServiceDescriptor(serviceDescriptor))

    override fun resolvedMethods(): Map<String, ResolvedServiceMethod<*, *>> = resolvedMethods
    override fun create(context: EventSourcedContext?): EventSourcedEntityHandler =
            EntityHandler(entityClass, context, anySupport, behavior)

    class EntityHandler(
            private val entityClass: KClass<*>,
            context: EventSourcedContext?,
            private val anySupport: AnySupport,
            private val behavior: KotlinEventBehaviorReflection) : EventSourcedEntityHandler {

        private val log = logger()
        private val reflectionHelper: ReflectionHelper = ReflectionHelper()
        private val entity = entityConstructor(DelegatingEventSourcedContext(context!!))

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
            val payload = anySupport.decode(command)
            val commandHandlers = behavior.commandHandlers

            if (!commandHandlers.contains(context?.commandName())) {
                throw RuntimeException(
                        "No command handler found for command [${context?.commandName()}] on ${entity.javaClass.name}"
                )
            }

            log.debug("Calling CommandHandlerInvoker in Entity: $entity")
            return commandHandlers[context?.commandName().toString()]?.invoke(entity, payload!!, context!!)
        }

        override fun handleSnapshot(snapshot: Any?, context: SnapshotContext?) {
            val payload = anySupport.decode(snapshot)
            val handler = behavior.getCachedSnapshotHandlerForClass(payload.javaClass)
            when (handler.isPresent) {
                true -> {
                    handler.get().invoke(entity, payload!!, context!!)
                }
                false -> throw RuntimeException(
                        "No snapshot handler found for event ${payload.javaClass} on ${entity.javaClass.name}")
            }
        }

        override fun snapshot(context: SnapshotContext?): Optional<Any> =
            when(behavior.snapshotInvoker.isPresent){
                true -> {
                    behavior.snapshotInvoker.get().invoke(entity, context!!)
                }
                false -> throw RuntimeException(
                        "No snapshot invoker found for entity ${entity.javaClass.name}")
            }


        private fun entityConstructor(context: EventSourcedEntityCreationContext): kotlin.Any {
            //todo: Verified if this is a single factory or prototype
            log.debug("Call constructor in ${entityClass.qualifiedName}")
            val constructors = entityClass.java.constructors
            if (constructors?.isNotEmpty()!! && constructors.size == 1) {
                return EntityConstructorInvoker(reflectionHelper.ensureAccessible(constructors[0]), context).invoke()
            }
            throw RuntimeException("Only a single constructor is allowed on event sourced entities: $entityClass")
        }

    }

    class EntityConstructorInvoker(
            private val constructor: Constructor<*>,
            private val context: EventSourcedEntityCreationContext)  {
        private val log = logger()
        private val params = constructor.parameters

        fun invoke(): kotlin.Any {
            if (params.isEmpty()) {
                return constructor.newInstance()
            }

            val args:List<kotlin.Any?> = params.map {
                getArguments(it)
            }.toList()

            log.trace("Constructor args size: ${params.size}. Params found: ${args.size}")
            return constructor.newInstance(*args.toTypedArray())
        }

        private fun getArguments(it: Parameter): kotlin.Any? = when {
            isEntityId(it) -> {
                if (!it.type.isAssignableFrom(String::class.java) || !it.type.isAssignableFrom(java.lang.String::class.java)) {
                    log.warn("Type of parameter annotated with @EntityId. Type: ${it.type}")
                    throw RuntimeException("@EntityId annotated parameter has type ${it.type}, must be String.")
                }
                log.debug("Type of parameter annotated with @EntityId. " +
                        "Type: ${it.type}. Value param type: ${context.entityId().javaClass}. Value: ${context.entityId()}")
                it.type.cast(context.entityId())
            }
            it.type.isAssignableFrom(context.javaClass) -> {
                context
            }
            else -> {
                log.trace("Param $it. Name: ${it.name} Type: ${it.type}")
                null
            }
        }

        private fun isEntityId(parameter: Parameter): Boolean {
            return parameter.declaredAnnotations.isNotEmpty() && parameter.isAnnotationPresent(EntityId::class.java) ||
                    parameter.declaredAnnotations.isNotEmpty() && parameter.isAnnotationPresent(io.cloudstate.javasupport.EntityId::class.java)
        }

    }

    class EventHandlerInvoker(private val method: Method, private val reflectionHelper: ReflectionHelper){
        private val log = logger()

        fun invoke(entity: kotlin.Any?, event: kotlin.Any, ctx: DelegatingEventContext?) {
            val parameters = reflectionHelper.getParameters(method, event, ctx!!)
            log.debug("EventHandlerInvoker method ${method.name} with ${method.parameterCount} params")
            method.invoke(entity, *parameters)
        }
    }

    class CommandHandlerInvoker(
            private val method: Method,
            private val anySupport: AnySupport,
            private val serviceMethod: ResolvedServiceMethod<*, *>,
            private val reflectionHelper: ReflectionHelper){
        private val log = logger()

        private val name = serviceMethod.method().fullName
        private val outputType = serviceMethod.method().outputType

        fun invoke(entityInstance: kotlin.Any?, command: kotlin.Any, context: CommandContext): Optional<Any>  {
            log.debug("CommandHandlerInvoker service method $name with Entity method ${method.name} " +
                    "in ${entityInstance?.javaClass?.name}. " +
                    "Declared Class Method: ${method.declaringClass}")

            val parameters = reflectionHelper.getParameters(method, command, context, anySupport);
            var result: kotlin.Any? = null
            log.debug("CommandHandlerInvoker method ${method.name} with ${method.parameterCount} params")

            if (method.parameterCount == 0) {
                result = method.invoke(entityInstance)
                return handleResult(result, outputType)
            }


            parameters.forEach {
                log.debug("CommandHandlerInvoker invoke with param name ${it?.javaClass?.typeName}. Value $it")
            }
            try{
                result = method.invoke(entityInstance, *parameters)
            }catch (e: Throwable) {
                log.warn("Handle FailInvoke (CommandContext.fail(...)) or Generic Failure occurred")
                if (log.isDebugEnabled){
                    e.printStackTrace()
                }
                return handleResult(result, outputType)
            }
            return handleResult(result, outputType)
        }

        private fun handleResult(result: kotlin.Any?, outputType: Descriptors.Descriptor): Optional<Any> =
                if (result == null) Optional.empty() else Optional.of(Any.pack(result as Message))

    }

    class SnapshotHandlerInvoker(private val method: Method, private val reflectionHelper: ReflectionHelper) {
        fun invoke(entityInstance: kotlin.Any, payload: kotlin.Any?, context: SnapshotContext) {
            val parameters = reflectionHelper.getParameters(method, payload, context)
            if (parameters.isNotEmpty()){
                method.invoke(entityInstance, *parameters)
            }
        }
    }

    class SnapshotInvoker(private val method: Method, private val reflectionHelper: ReflectionHelper) {
        fun invoke(entityInstance: kotlin.Any?, context: SnapshotContext): Optional<Any> {
            val parameters = reflectionHelper.getParameters(method, null, context)
            if (parameters.isEmpty()){
                return Optional.of(Any.pack(method.invoke(entityInstance) as Message))
            }
            return Optional.of(Any.pack(method.invoke(entityInstance, parameters) as Message))
        }
    }

    class DelegatingEventContext(private val delegate: EventContext) : EventContext {
        override fun entityId(): String = delegate.entityId()

        override fun serviceCallFactory(): ServiceCallFactory = delegate.serviceCallFactory()

        override fun sequenceNumber(): Long = delegate.sequenceNumber()

    }

    class DelegatingEventSourcedContext(private val delegate: EventSourcedContext) : EventSourcedEntityCreationContext {
        override fun entityId(): String = delegate.entityId()
        override fun serviceCallFactory(): ServiceCallFactory = delegate.serviceCallFactory()
    }
}