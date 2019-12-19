package io.cloudstate.kotlinsupport.services.eventsourced

import io.cloudstate.kotlinsupport.Context
import io.cloudstate.kotlinsupport.services.StatefulService

abstract class EventSourcedEntity: StatefulService {
    private var context: EventSourcedEntityHandlerContext? = null

    override fun setContext(context: Context) {
        this.context = context as EventSourcedEntityHandlerContext
    }

    override fun fail(obj: Any) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override infix fun emit(item: Any) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override infix fun forward(item: Any) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    inline fun <reified T> snapshot(crossinline body: () -> T) {

    }

    inline fun <reified T> handleSnapshot(crossinline body: (T) -> Unit) {}

    inline fun <reified T> eventHandler(crossinline body: (T) -> Unit) {}

    inline fun <reified T> commandResultHandler(crossinline body: () -> T) {}

    inline fun <reified T> commandActionHandler(crossinline body: (T) -> Any) {}

    abstract fun create(): Handler

    class Handler {

        lateinit var snapshot: () -> Any
        lateinit var handleSnapshot: (Any) -> Unit
        lateinit var eventHandlers: List<(Any) -> Unit>

        lateinit var commandResultHandlers: List<() -> Any> 
        lateinit var commandActionHandlers: List<(Any) -> Any> 

    }

}