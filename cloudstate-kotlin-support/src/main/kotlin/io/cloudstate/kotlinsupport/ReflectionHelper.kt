package io.cloudstate.kotlinsupport

import com.google.protobuf.GeneratedMessageV3
import io.cloudstate.javasupport.Context
import io.cloudstate.javasupport.eventsourced.CommandContext
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Member
import java.lang.reflect.Method
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
        return clazz.java.declaredMethods.toSet()
    }

    fun getCapitalizedName(member: Member): String =
        if (member.name[0].isLowerCase()) {
            member.name[0].toUpperCase() + member.name.drop(1)
        } else member.name

    fun getParameters(method: Method, command: com.google.protobuf.Any, context: CommandContext): Array<Any> {
        var args = mutableListOf<Any>()

        if (method.parameters.isEmpty()) {
            return arrayOf()
        }

        method.parameters.forEach { param ->
            if (param.type.isAssignableFrom(CommandContext::class.java)) {
                args.add(context)
            }

            if (param.type.isAssignableFrom(GeneratedMessageV3::class.java)) {
                val message = com.google.protobuf.Any.parseFrom(command.value)
                args.add(message)
            }
        }
        return args.toTypedArray()
    }

}
