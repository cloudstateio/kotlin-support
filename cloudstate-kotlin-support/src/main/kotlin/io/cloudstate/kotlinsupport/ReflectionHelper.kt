package io.cloudstate.kotlinsupport

class ReflectionHelper {

    private val log = logger()

    fun getAllMethodsAnnotatedBy(type: Class<*>, annotationClass: Class<out kotlin.Annotation>): MutableList<Map<String, Annotation>> {
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
