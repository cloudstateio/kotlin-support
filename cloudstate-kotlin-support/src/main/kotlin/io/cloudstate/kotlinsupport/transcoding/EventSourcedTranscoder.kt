package io.cloudstate.kotlinsupport.transcoding

import io.cloudstate.javasupport.eventsourced.EventSourcedEntity as JEventSourcedEntity

import io.cloudstate.kotlinsupport.api.eventsourced.*
import io.cloudstate.kotlinsupport.logger
import io.cloudstate.kotlinsupport.transcoding.eventsourced.*
import net.bytebuddy.ByteBuddy
import net.bytebuddy.agent.ByteBuddyAgent
import net.bytebuddy.asm.MemberAttributeExtension
import net.bytebuddy.dynamic.DynamicType
import net.bytebuddy.dynamic.loading.ClassReloadingStrategy
import net.bytebuddy.matcher.ElementMatchers

class EventSourcedTranscoder(private val clazz: Class<*>): Transcoder {
    private val log = logger()

    init {
        log.debug("Initializing ByteBuddy Agent....")
        ByteBuddyAgent.install()
    }

    override fun transcode(): Class<*>? = transcode(clazz)

    private fun transcode(clazz: Class<*>): Class<out Any>? {

        when {
            // Return if type already is Cloudstate entity type
            clazz.getAnnotation(JEventSourcedEntity::class.java) != null -> return clazz

            else -> {
                log.info("Executing Transformer...")
                val snapshotMethods = getAllMethodsAnnotatedBy(clazz, Snapshot::class.java)
                val eventHandlertMethods = getAllMethodsAnnotatedBy(clazz, EventHandler::class.java)
                val snapshotHandlerMethods = getAllMethodsAnnotatedBy(clazz, SnapshotHandler::class.java)
                val commandHandlerMethods = getAllMethodsAnnotatedBy(clazz, CommandHandler::class.java)

                val classReloadingStrategy = ClassReloadingStrategy(
                        ByteBuddyAgent.getInstrumentation(),
                        ClassReloadingStrategy.Strategy.REDEFINITION)

                var builder: DynamicType.Builder<out Any>? =
                        createEntityAnnotation(clazz, classReloadingStrategy)

                snapshotMethods.forEach {
                    it.forEach { (method, _) ->
                        builder = builder
                                ?.visit(MemberAttributeExtension.ForMethod()
                                        .annotateMethod(SnapshotImpl())
                                        .on(ElementMatchers.named(method)))
                    }
                }

                snapshotHandlerMethods.forEach {
                    it.forEach { (method, _) ->
                        builder = builder
                                ?.visit(MemberAttributeExtension.ForMethod()
                                        .annotateMethod(SnapshotHandlerImpl())
                                        .on(ElementMatchers.named(method)))
                    }
                }

                eventHandlertMethods.forEach {
                    it.forEach { (method, _) ->
                        builder = builder
                                ?.visit(MemberAttributeExtension.ForMethod()
                                        .annotateMethod(EventHandlerImpl())
                                        .on(ElementMatchers.named(method)))
                    }
                }

                commandHandlerMethods.forEach {
                    it.forEach { (method, annotation) ->
                        var cmdHandlerAnnotation = annotation as CommandHandler
                        builder = builder
                                ?.visit(MemberAttributeExtension.ForMethod()
                                        .annotateMethod(CommandHandlerImpl(cmdHandlerAnnotation.name))
                                        .on(ElementMatchers.named(method)))
                    }
                }

                return builder
                        ?.make()
                        ?.load(this.clazz.classLoader, classReloadingStrategy)
                        ?.loaded
            }
        }

    }

    private fun createEntityAnnotation(clazz: Class<*>, classReloadingStrategy: ClassReloadingStrategy): DynamicType.Builder<out Any>? {

        val eventSourcedEntityAnnotation = clazz.getAnnotation(EventSourcedEntity::class.java)

        when {eventSourcedEntityAnnotation != null -> {
            return  ByteBuddy()
                    .redefine(clazz)
                    .annotateType(
                            mutableListOf(
                                    EventSourcedEntityImpl(
                                            eventSourcedEntityAnnotation.persistenceId,
                                            eventSourcedEntityAnnotation.snapshotEvery)))


            /*.defineField("persistenceId", String.javaClass, Visibility.PRIVATE)
            .defineConstructor(Visibility.PUBLIC).withParameter(String.javaClass)
            .annotateParameter(EntityIdImpl())
            .intercept(MethodCall.invoke(clazz.getConstructor(String.javaClass))
                .withArgument(1)
                        .andThen(FieldAccessor.ofField("persistenceId")
                                .setsArgumentAt(1)))*/

        }
            else -> {
                return  ByteBuddy()
                        .redefine(clazz)
            }
        }
    }

    private fun getAllMethodsAnnotatedBy(type: Class<*>, annotationClass: Class<out kotlin.Annotation>): MutableList<Map<String, Annotation>> {
        var methods:MutableList<Map<String, Annotation>> = mutableListOf<Map<String, Annotation>>()

        log.debug("Found ${type.methods.filter { it.isAnnotationPresent(annotationClass)  }.size} methods to processing...")
        type.methods.filter { it.isAnnotationPresent(annotationClass) }.forEach {
            log.debug("Found Method ${it.name} annotated with ${annotationClass.simpleName}. ReturnType ${it.returnType} GenericReturnTYpe ${it.genericReturnType}")
            var methodAndAnnotation = mapOf<String, Annotation>(it.name to it.getAnnotation(annotationClass))
            methods.add(methodAndAnnotation)
        }

        log.debug("${methods.size} Annotations of type ${annotationClass.simpleName} found in ${type.simpleName}")
        return methods
    }
}