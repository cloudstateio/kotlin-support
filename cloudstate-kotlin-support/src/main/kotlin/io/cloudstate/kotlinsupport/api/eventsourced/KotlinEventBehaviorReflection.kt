package io.cloudstate.kotlinsupport.api.eventsourced

import io.cloudstate.javasupport.Context
import io.cloudstate.javasupport.impl.AnySupport
import io.cloudstate.javasupport.impl.ResolvedServiceMethod
import io.cloudstate.kotlinsupport.ReflectionHelper
import io.cloudstate.kotlinsupport.annotations.eventsourced.CommandHandler
import io.cloudstate.kotlinsupport.annotations.eventsourced.EventHandler
import io.cloudstate.kotlinsupport.annotations.eventsourced.Snapshot
import io.cloudstate.kotlinsupport.annotations.eventsourced.SnapshotHandler
import io.cloudstate.kotlinsupport.logger
import java.lang.reflect.Method
import java.util.*
import kotlin.reflect.KClass

class KotlinEventBehaviorReflection(
        entityClass: KClass<*>,
        private val resolvedMethods: scala.collection.immutable.Map<String, ResolvedServiceMethod<*, *>>,
        private val anySupport: AnySupport) {
    private val log = logger()
    private val reflectionHelper: ReflectionHelper = ReflectionHelper()
    private val allMethods = reflectionHelper.getAllDeclaredMethods(entityClass)

    val snapshotInvoker = findSnapshotInvoker()
    private val eventHandlers = findEventHandlers()
    val commandHandlers = findCommandHandlers()
    val snapshotHandlers  = findSnapshotHandlers()

    init {
        allMethods.forEach {
            log.debug("AllMethods::Method -> ${it.name}")
        }
    }

    fun getCachedEventHandlerForClass(clazz: Class<*>): Optional<KotlinAnnotationBasedEventSourced.EventHandlerInvoker> =
        Optional.ofNullable(eventHandlers[clazz])

    fun getCachedSnapshotHandlerForClass(clazz: Class<*>): Optional<KotlinAnnotationBasedEventSourced.SnapshotHandlerInvoker> =
        Optional.ofNullable(snapshotHandlers[clazz])

    private fun findEventHandlers(): Map<Class<*>, KotlinAnnotationBasedEventSourced.EventHandlerInvoker> =
        allMethods
            .filter { it.isAnnotationPresent(EventHandler::class.java)  }
            .map { method -> Pair(
                    getEventClass(method, method.getAnnotation(EventHandler::class.java)),
                    KotlinAnnotationBasedEventSourced.EventHandlerInvoker(reflectionHelper.ensureAccessible(method), reflectionHelper)) }
            .toMap()

    private fun findCommandHandlers(): Map<String, KotlinAnnotationBasedEventSourced.CommandHandlerInvoker> =
        allMethods
            .filter { it.isAnnotationPresent(CommandHandler::class.java) }
            .map { method ->
                val annotation = method.getAnnotation(CommandHandler::class.java) as CommandHandler
                val name: String = if (annotation.name.isEmpty()) {
                    reflectionHelper.getCapitalizedName(method)
                } else annotation.name

                val serviceMethod: ResolvedServiceMethod<*,*> = resolvedMethods.getOrElse(name) {
                    throw RuntimeException("Command handler method ${method.name} for command $name found, but the service has no command by that name.")
                }

                Pair(name,
                        KotlinAnnotationBasedEventSourced
                                .CommandHandlerInvoker(
                                        reflectionHelper.ensureAccessible(method), anySupport, serviceMethod, reflectionHelper))
            }.toMap()

    private fun findSnapshotHandlers(): Map<Class<*>, KotlinAnnotationBasedEventSourced.SnapshotHandlerInvoker> =
        allMethods
            .filter { it.isAnnotationPresent(SnapshotHandler::class.java) }
            .map { method -> Pair(
                    getSnapshotHandlerClass(method, method.getAnnotation(SnapshotHandler::class.java)),
                    KotlinAnnotationBasedEventSourced.SnapshotHandlerInvoker(reflectionHelper.ensureAccessible(method), reflectionHelper)) }
            .toMap()

    private fun findSnapshotInvoker(): Optional<KotlinAnnotationBasedEventSourced.SnapshotInvoker> {
        val map = allMethods
                .filter { it.isAnnotationPresent(Snapshot::class.java) }
                .map { method -> KotlinAnnotationBasedEventSourced.SnapshotInvoker(reflectionHelper.ensureAccessible(method), reflectionHelper) }

        if (map.size > 1) {
            //Todo: Better message here
            throw RuntimeException("Multiple snapshoting methods found on Entity function")
        }
        return Optional.of(map[0])
    }

    private fun getEventClass(method: Method, annotation: Annotation): Class<*> {
        val evtAnn = annotation as EventHandler

        if (method.parameters.filter { param -> !param.type.isInstance(Context::class.java)  }
                        .size > 1) {
            throw  RuntimeException("Event handling method ${method.name} cannot contain more than one type parameter. " +
                    "Parameters ${method.parameters}")
        }

        val parameter = method.parameters.filter { param -> !param.type.isInstance(Context::class.java) }[0]

        if ((parameter.javaClass != evtAnn.eventClass.java) && (evtAnn.eventClass.java != Object::class.java) ) {
            return evtAnn.eventClass.java
        }
        log.debug("EventHandler for method $method found. Parameter: $parameter.  Class: ${parameter.type}")
        return parameter.type
    }

    private fun getSnapshotHandlerClass(method: Method, annotation: Annotation): Class<*> {
        val evtAnn = annotation as SnapshotHandler

        if (method.parameters.filter { param -> !param.type.isInstance(Context::class.java)  }
                        .size > 1) {
            throw  RuntimeException("Event handling method ${method.name} cannot contain more than one type parameter. " +
                    "Parameters ${method.parameters}")
        }

        val parameter = method.parameters.filter { param -> !param.type.isInstance(Context::class.java) }[0]
        log.debug("SnapshotHandler for method $method found. Parameter: $parameter.  Class: ${parameter.type}")
        return parameter.type
    }

    private fun getSnapshotClass(method: Method, annotation: Annotation): Class<*> {
        val evtAnn = annotation as Snapshot

        if (method.parameters.filter { param -> !param.type.isInstance(Context::class.java)  }
                        .size > 1) {
            throw  RuntimeException("Event handling method ${method.name} cannot contain more than one type parameter. " +
                    "Parameters ${method.parameters}")
        }

        val parameter = method.parameters.filter { param -> !param.type.isInstance(Context::class.java) }[0]
        log.debug("Snapshot for method $method found. Parameter: $parameter.  Class: ${parameter.type}")
        return parameter.type
    }

}
