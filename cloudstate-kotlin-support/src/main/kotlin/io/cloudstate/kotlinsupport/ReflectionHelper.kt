package io.cloudstate.kotlinsupport

import com.google.protobuf.GeneratedMessageV3
import io.cloudstate.javasupport.eventsourced.CommandContext
import io.cloudstate.javasupport.impl.AnySupport
import io.cloudstate.kotlinsupport.api.eventsourced.KotlinAnnotationBasedEventSourced
import java.lang.reflect.*
import kotlin.reflect.KClass

class ReflectionHelper {

    private val log = logger()

    fun getAllMethodsAnnotatedBy(type: Class<*>, annotationClass: Class<out kotlin.Annotation>): MutableList<Map<String, Annotation>> {
        var methods: MutableList<Map<String, Annotation>> = mutableListOf<Map<String, Annotation>>()

        log.debug("Found ${type.methods.filter { it.isAnnotationPresent(annotationClass) }.size} methods to processing...")
        type.methods.filter { it.isAnnotationPresent(annotationClass) }.forEach {
            log.debug("Found Method ${it.name} annotated with ${annotationClass.simpleName}. ReturnType ${it.returnType} GenericReturnTYpe ${it.genericReturnType}")
            var methodAndAnnotation = mapOf<String, Annotation>(it.name to it.getAnnotation(annotationClass))
            methods.add(methodAndAnnotation)
        }

        log.debug("${methods.size} Annotations of type ${annotationClass.simpleName} found in ${type.simpleName}")
        return methods
    }

    fun <T: AccessibleObject> ensureAccessible(accessible: T): T {
        if (!accessible.isAccessible) {
            accessible.isAccessible = true
        }
        return accessible
    }

    fun getAllDeclaredMethods(clazz: KClass<*>): Set<Method> {
        log.debug("Process Runtime class ${clazz.qualifiedName} type: ${clazz.java}")
        return clazz.java.declaredMethods.filter { method -> Modifier.isPublic(method.modifiers) }.toSet()
    }

    fun getCapitalizedName(member: Member): String =
        if (member.name[0].isLowerCase()) {
            member.name[0].toUpperCase() + member.name.drop(1)
        } else member.name

    fun getParameters(method: Method, command: com.google.protobuf.Any, context: CommandContext, anySupport: AnySupport): Array<Any?> {
        if (method.parameters.isEmpty()) {
            return arrayOf()
        }

        val args:List<kotlin.Any?> = method.parameters.map {
            getMethodArgs(it, command, context, anySupport)
        }.toList()

        return args.toTypedArray()
    }

    fun getParameters(method: Method, event: kotlin.Any, context: KotlinAnnotationBasedEventSourced.DelegatingEventContext): Array<Any?> {
        if (method.parameters.isEmpty()) {
            return arrayOf()
        }

        val args:List<kotlin.Any?> = method.parameters.map {
            getMethodArgs(it, event, context)
        }.toList()

        return args.toTypedArray()
    }

    private fun getMethodArgs(it: Parameter, event: kotlin.Any, context: KotlinAnnotationBasedEventSourced.DelegatingEventContext): kotlin.Any? = when {
        it.type.isAssignableFrom(context.javaClass) -> {
             context
        }

        it.type.isAssignableFrom(GeneratedMessageV3::class.java) -> {
            event
        }
        else -> null
    }

    private fun getMethodArgs(it: Parameter, command: com.google.protobuf.Any, context: CommandContext, anySupport: AnySupport): kotlin.Any? {
        log.debug("Parameter $it")
        return when {
            it.type.isAssignableFrom(context.javaClass) -> {
                log.debug("Found parameter type context.")
                context
            }

            GeneratedMessageV3::class.java.isAssignableFrom(it.type) -> {
                log.debug("Found parameter type Object.")
                anySupport.decode(command)
                //val unpack = command.unpack(clazz)
                //com.google.protobuf.Any.parseFrom(command.value)
            }
            else -> null
        }
    }

}
