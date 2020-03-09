package io.cloudstate.kotlinsupport.transcoding

import io.cloudstate.kotlinsupport.ReflectionHelper
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
    private val helper = ReflectionHelper()

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
                val commandHandlerMethods = helper.getAllMethodsAnnotatedBy(clazz, CommandHandler::class.java)

                val classReloadingStrategy = ClassReloadingStrategy(
                        ByteBuddyAgent.getInstrumentation(),
                        ClassReloadingStrategy.Strategy.REDEFINITION)

                var builder: DynamicType.Builder<out Any>? =
                        createEntityAnnotation(clazz, classReloadingStrategy, CrdtEntity::class.java)

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

    private fun createEntityAnnotation(clazz: Class<*>, classReloadingStrategy: ClassReloadingStrategy, annotation: Class<out Annotation>): DynamicType.Builder<out Any>? {

        val entityAnnotation = clazz.getAnnotation(annotation)

        return when { entityAnnotation != null -> {
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

}