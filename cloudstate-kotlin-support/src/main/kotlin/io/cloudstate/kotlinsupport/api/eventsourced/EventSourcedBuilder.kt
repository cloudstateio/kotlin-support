package io.cloudstate.kotlinsupport.api.eventsourced

import io.cloudstate.javasupport.eventsourced.EventSourcedEntity as JEventSourcedEntity

import io.cloudstate.kotlinsupport.logger
import io.cloudstate.kotlinsupport.transcoding.eventsourced.CommandHandlerImpl
import io.cloudstate.kotlinsupport.transcoding.eventsourced.EventHandlerImpl
import io.cloudstate.kotlinsupport.transcoding.eventsourced.SnapshotHandlerImpl
import io.cloudstate.kotlinsupport.transcoding.eventsourced.SnapshotImpl
import net.bytebuddy.ByteBuddy
import net.bytebuddy.agent.ByteBuddyAgent
import net.bytebuddy.description.annotation.AnnotationDescription
import net.bytebuddy.description.modifier.Visibility
import net.bytebuddy.dynamic.DynamicType
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy
import net.bytebuddy.implementation.FixedValue
import net.bytebuddy.implementation.MethodDelegation
import net.bytebuddy.matcher.ElementMatchers.named
import kotlin.reflect.jvm.javaType

fun <T: Any>eventSourcedEntityBuilder(handler: EventSourcedHandler<T>.() -> Unit): EventSourcedBuilder<T> {
    val eventSourcedHandler = EventSourcedHandler<T>()
    eventSourcedHandler.handler()
    return EventSourcedBuilder(eventSourcedHandler)
}

class EventSourcedBuilder<T : Any>(private val handler: EventSourcedHandler<T>) {
    private val log = logger()

    init {
        log.debug("Initializing ByteBuddy Agent....")
        ByteBuddyAgent.install()
    }

    fun build(): Class<*>? {

        val eventSourcedEntity: AnnotationDescription = createEventSourcedEntityAnnotation()
        var builder: DynamicType.Builder<out Any>? = ByteBuddy()
                .subclass(Any::class.java)
                .name("EventSourcedEntityJava")
                .annotateType(mutableListOf(eventSourcedEntity))

        handler.getMethodDefinitions().forEach{ method ->

            when(method.evtSourcedMethodType) {
                EvtSourcedMethodType.SNAPSHOT -> {
                    val snapshotDefinition = method.lambdaDefinition
                    log.info("Return type is -> ${snapshotDefinition.returnType?.javaClass}")

                    builder = builder
                            ?.defineMethod(method.name, snapshotDefinition.returnType?.javaClass, Visibility.PUBLIC)//.withParameters(Void.class)
                            //?.intercept(MethodDelegation.to(EventSourcedInterceptor(method)))
                            ?.intercept(FixedValue.value("Test"))
                            ?.annotateMethod(SnapshotImpl())
                }

                EvtSourcedMethodType.SNAPSHOT_HANDLER -> {
                    builder = builder?.method(named(method.name))
                            ?.intercept(MethodDelegation.to(EventSourcedInterceptor(method)))
                            ?.annotateMethod(SnapshotHandlerImpl())
                }

                EvtSourcedMethodType.COMMAND_HANDLER -> {
                    builder = builder?.method(named(method.name))
                            ?.intercept(MethodDelegation.to(EventSourcedInterceptor(method)))
                            ?.annotateMethod(CommandHandlerImpl(method.name))
                }

                EvtSourcedMethodType.EVENT_HANDLER -> {
                    val eventDefinition = method.lambdaDefinition as EventHandlerFunction<*>
                    builder = builder?.method(named(method.name))
                            ?.intercept(MethodDelegation.to(EventSourcedInterceptor(method)))
                            ?.annotateMethod(EventHandlerImpl(eventDefinition.eventClass))
                }
            }

        }

        return builder
                ?.make()
                ?.load(this.javaClass.classLoader, ClassLoadingStrategy.Default.WRAPPER)
                ?.loaded
    }

    private fun createEventSourcedEntityAnnotation(): AnnotationDescription =
            AnnotationDescription.Builder.ofType(JEventSourcedEntity::class.java)
                    .define("persistenceId", handler.persistenceId)
                    .define("snapshotEvery", handler.snapshotEvery!!)
                    .build()

}

class EventSourcedInterceptor(val methodDefinition: MethodDefinition) {

}