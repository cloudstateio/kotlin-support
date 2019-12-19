package io.cloudstate.kotlinsupport.annotations

import kotlin.reflect.KClass

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.EXPRESSION)
@Retention(AnnotationRetention.SOURCE)
annotation class EventHandler(val value: String)
