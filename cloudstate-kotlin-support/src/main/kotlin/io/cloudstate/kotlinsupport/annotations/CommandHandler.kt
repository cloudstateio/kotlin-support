package io.cloudstate.kotlinsupport.annotations

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.EXPRESSION)
@Retention(AnnotationRetention.SOURCE)
annotation class CommandHandler(val value: String)
