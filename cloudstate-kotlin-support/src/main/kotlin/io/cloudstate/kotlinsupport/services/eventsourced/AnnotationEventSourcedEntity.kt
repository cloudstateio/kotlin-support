package io.cloudstate.kotlinsupport.services.eventsourced

import io.cloudstate.kotlinsupport.Context
import io.cloudstate.kotlinsupport.services.StatefulService

open class AnnotationEventSourcedEntity(val entityId: String): StatefulService {

    override fun setContext(context: Context) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun fail(obj: Any) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun emit(obj: Any) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun forward(obj: Any) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}