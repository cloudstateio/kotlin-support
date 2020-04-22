package io.cloudstate.kotlinsupport

import io.cloudstate.javasupport.Context
import io.cloudstate.javasupport.eventsourced.CommandContext
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Member
import java.lang.reflect.Method

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

    fun getAllDeclaredMethods(clazz: Class<*>): Set<Method>  =
        if (clazz.superclass == null ) {
            clazz.declaredMethods.toSet()
        } else {
            clazz.declaredMethods.toSet().union(getAllDeclaredMethods(clazz.superclass))
        }

    fun getCapitalizedName(member: Member): String =
        if (member.name[0].isLowerCase()) {
            member.name[0].toUpperCase() + member.name.drop(1)
        } else member.name

    fun getParameters(method: Method, command: com.google.protobuf.Any, context: CommandContext): Array<Any> {
        command.typeUrl
        command.value
        var args= arrayOf<Any>()
        for (Parameter in method.parameters)
        method.parameters.forEach { param ->
            if (param.type.isAssignableFrom(Context::class.java)) {
                args

            }
        }
        return listOf<Any>("").toTypedArray()
    }

}
