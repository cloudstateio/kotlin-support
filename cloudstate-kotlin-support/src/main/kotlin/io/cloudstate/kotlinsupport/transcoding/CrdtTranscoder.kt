package io.cloudstate.kotlinsupport.transcoding

import io.cloudstate.javasupport.crdt.CrdtEntity as JCrdtEntity

import io.cloudstate.kotlinsupport.api.crdt.*
import io.cloudstate.kotlinsupport.logger
import io.cloudstate.kotlinsupport.transcoding.crdt.*
import net.bytebuddy.ByteBuddy
import net.bytebuddy.agent.ByteBuddyAgent
import net.bytebuddy.asm.MemberAttributeExtension
import net.bytebuddy.dynamic.DynamicType
import net.bytebuddy.dynamic.loading.ClassReloadingStrategy
import net.bytebuddy.matcher.ElementMatchers

class CrdtTranscoder(private val clazz: Class<*>): Transcoder {
    private val log = logger()

    init {
        log.debug("Initializing ByteBuddy Agent....")
        ByteBuddyAgent.install()
    }

    override fun transcode(): Class<*>? = transcode(clazz)

    private fun transcode(clazz: Class<*>): Class<out Any>? {

        when {
            // Return if type already is Cloudstate entity type
            clazz.getAnnotation(JCrdtEntity::class.java) != null -> return clazz

            else -> {
                log.info("Executing Transformer...")
                val commandHandlerMethods = getAllMethodsAnnotatedBy(clazz, CommandHandler::class.java)

                val classReloadingStrategy = ClassReloadingStrategy(
                        ByteBuddyAgent.getInstrumentation(),
                        ClassReloadingStrategy.Strategy.REDEFINITION)

                var builder: DynamicType.Builder<out Any>? =
                        createEntityAnnotation(clazz, classReloadingStrategy)

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

        val crdtEntityAnnotation = clazz.getAnnotation(CrdtEntity::class.java)

        return when { crdtEntityAnnotation != null -> {
            ByteBuddy()
                    .redefine(clazz)
                    .annotateType(
                            mutableListOf(
                                    CrdtEntityImpl()))
        }
            else -> {
                ByteBuddy()
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