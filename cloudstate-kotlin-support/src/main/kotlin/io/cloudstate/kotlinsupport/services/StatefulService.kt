package io.cloudstate.kotlinsupport.services

import io.cloudstate.kotlinsupport.Context

interface StatefulService {

    fun setContext(context: Context)
    infix fun fail(obj: Any)
    infix fun emit(obj: Any)
    infix fun forward(obj: Any)
}
