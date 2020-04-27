package io.cloudstate.kotlinsupport.tests

import io.cloudstate.javasupport.EntityId
import io.cloudstate.kotlinsupport.annotations.crdt.CommandHandler
import io.cloudstate.kotlinsupport.annotations.crdt.CrdtEntity
import io.cloudstate.kotlinsupport.api.transcoding.CrdtTranscoder

import io.cloudstate.kotlinsupport.logger
import net.bytebuddy.agent.ByteBuddyAgent

import org.junit.Test
import java.lang.reflect.Constructor
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import io.cloudstate.javasupport.crdt.CrdtEntity as JCrdtEntity

class CrdtTranscoderTest {

    private val log = logger()

    init {
        log.debug("Initializing ByteBuddy Agent....")
        ByteBuddyAgent.install()
    }

    @Test
    fun `List Names of Methods in Log`() {
        val transcoder = CrdtTranscoder(CrdtTestEntity::class.java)
        val targetClazzRepresentation: Class<*>? = transcoder.transcode()

        val ctor: Constructor<*>? = targetClazzRepresentation?.getConstructor(String::class.java)
        val entityInstance: CrdtTestEntity = ctor?.newInstance("entityId") as CrdtTestEntity

        val crdtEntity = entityInstance.javaClass.getAnnotation(JCrdtEntity::class.java)

        log.info("Entity type is not null. Type is ${crdtEntity?.javaClass?.simpleName}")
        assertNotNull(entityInstance)

        entityInstance.javaClass.annotations.forEach {
            log.info("Test Class annotation found. $it")
        }

        entityInstance.javaClass.methods.forEach {
            it.annotations.forEach { annotation ->
                log.info("Found annotation $annotation in ${it.name}")
            }
        }

        val methodNameInAnnotation = entityInstance.javaClass.methods.filter {
            it.name == "getCart"
        }[0].getAnnotation(CommandHandler::class.java).name

        assertEquals("otherGetCart", methodNameInAnnotation)
    }
}

@CrdtEntity
private class CrdtTestEntity(@param:EntityId private val entityId: String) {

    @CommandHandler(name = "otherGetCart")
    fun getCart(){}

    @CommandHandler
    fun addItem() {}

    @CommandHandler
    fun removeItem() {}
}