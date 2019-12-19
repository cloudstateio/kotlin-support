package io.cloudstate.kotlinsupport.services.crdt

import io.cloudstate.kotlinsupport.Context
import io.cloudstate.kotlinsupport.services.StatefulService

abstract class CrdtStatefulService: StatefulService {

    override fun setContext(context: Context) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun fail(obj: Any) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override infix fun emit(obj: Any) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override infix fun forward(obj: Any) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}