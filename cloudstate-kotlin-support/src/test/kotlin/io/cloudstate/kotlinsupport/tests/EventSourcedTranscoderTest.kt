package io.cloudstate.kotlinsupport.tests

import com.example.shoppingcart.persistence.Domain
import io.cloudstate.javasupport.EntityId

import io.cloudstate.kotlinsupport.api.eventsourced.*
import io.cloudstate.kotlinsupport.transcoding.EventSourcedTranscoder
import io.cloudstate.kotlinsupport.logger
import net.bytebuddy.agent.ByteBuddyAgent

import org.junit.Test
import java.lang.reflect.Constructor
import kotlin.system.measureTimeMillis
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import io.cloudstate.javasupport.eventsourced.EventSourcedEntity as JEventSourcedEntity

class EventSourcedTranscoderTest {

    private val log = logger()

    init {
        log.debug("Initializing ByteBuddy Agent....")
        ByteBuddyAgent.install()
    }

    @Test
    fun `List Names of Methods in Log`() {
        var transcoder: EventSourcedTranscoder

        var targetClazzRepresentation: Class<*>? = null

        val executionTime = measureTimeMillis {
            // block of code to be measured
            transcoder = EventSourcedTranscoder(TestEntity::class.java)
            targetClazzRepresentation = transcoder.transcode()
        }

        log.info("Transcode measured time -> $executionTime")

        val ctor: Constructor<*>? = targetClazzRepresentation?.getConstructor(String::class.java)
        val entityInstance: TestEntity = ctor?.newInstance("entityId") as TestEntity

        val eventSourcedEntity = entityInstance.javaClass.getAnnotation(JEventSourcedEntity::class.java)

        log.info("Entity type is not null. Type is ${eventSourcedEntity?.javaClass?.simpleName}")
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

@EventSourcedEntity(persistenceId = "persistenceId", snapshotEvery = 1)
class TestEntity(@param:EntityId private val entityId: String) {

    @Snapshot
    fun snapshot() {}

    @SnapshotHandler
    fun handleSnapshot(cart: Domain.Cart) {}

    @EventHandler
    fun itemAdded(itemAdded: Domain.ItemAdded) {}

    @EventHandler
    fun itemRemoved(itemRemoved: Domain.ItemRemoved){}

    @CommandHandler(name = "otherGetCart")
    fun getCart(){}

    @CommandHandler
    fun addItem() {}

    @CommandHandler
    fun removeItem() {}
}