package io.cloudstate.kotlinsupport.api.eventsourced

import io.cloudstate.javasupport.Context
import io.cloudstate.javasupport.impl.ResolvedServiceMethod
import io.cloudstate.kotlinsupport.ReflectionHelper
import io.cloudstate.kotlinsupport.annotations.eventsourced.CommandHandler
import io.cloudstate.kotlinsupport.annotations.eventsourced.EventHandler
import java.lang.reflect.Method
import java.util.*

class KotlinEventBehaviorReflection(private val entityClass: Class<*>, private val resolvedMethods: scala.collection.immutable.Map<String, ResolvedServiceMethod<*, *>>) {
    private val reflectionHelper: ReflectionHelper = ReflectionHelper()
    private val allMethods = reflectionHelper.getAllDeclaredMethods(entityClass)

    private val snapshotInvoker = getSnapshotInvoker()
    private val eventHandlers = getEventHandlers()
    private val commandHandlers = getCommandHandlers()
    private val snapshotHandlers  = getSnapshotHandlers()

    fun getCachedEventHandlerForClass(clazz: Class<*>): Optional<KotlinAnnotationBasedEventSourced.EventHandlerInvoker> {
        //Todo: Get EventHandlerInvoker for type of event
        return Optional.empty<KotlinAnnotationBasedEventSourced.EventHandlerInvoker>()
    }

    private fun getEventHandlers(): Map<Class<*>, KotlinAnnotationBasedEventSourced.EventHandlerInvoker> =
        allMethods
                .filter { it.isAnnotationPresent(EventHandler::class.java)  }
                .map { method -> Pair(
                        getEventClass(method, method.getAnnotation(EventHandler::class.java)),
                        KotlinAnnotationBasedEventSourced.EventHandlerInvoker(reflectionHelper.ensureAccessible(method), reflectionHelper)) }
                .toMap()

    private fun getCommandHandlers(): Map<String, KotlinAnnotationBasedEventSourced.CommandHandlerInvoker> =
        allMethods
                .filter { it.isAnnotationPresent(CommandHandler::class.java) }
                .map { method ->
                    val annotation = method.getAnnotation(CommandHandler::class.java) as CommandHandler
                    val name: String = if (annotation.name.isEmpty()) {
                        reflectionHelper.getCapitalizedName(method)
                    } else annotation.name

                    val serviceMethod = resolvedMethods.getOrElse(name) {
                        throw RuntimeException("Command handler method ${method.name} for command $name found, but the service has no command by that name.")
                    }

                    Pair(name,
                            KotlinAnnotationBasedEventSourced.CommandHandlerInvoker(reflectionHelper.ensureAccessible(method),serviceMethod, reflectionHelper))
                }.toMap()

    private fun getSnapshotInvoker(): Optional<KotlinAnnotationBasedEventSourced.SnapshotInvoker> {
        TODO("Not yet implemented")
    }

    private fun getSnapshotHandlers(): Map<Class<*>, KotlinAnnotationBasedEventSourced.SnapshotHandlerInvoker> {
        TODO("Not yet implemented")
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
        return parameter.javaClass
    }

}
