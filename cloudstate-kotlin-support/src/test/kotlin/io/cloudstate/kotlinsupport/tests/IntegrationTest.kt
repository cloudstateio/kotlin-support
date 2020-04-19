package io.cloudstate.kotlinsupport.tests

import akka.actor.ActorSystem
import akka.grpc.GrpcClientSettings
import akka.stream.ActorMaterializer
import akka.stream.Materializer
import com.example.shoppingcart.ShoppingCartClient
import com.example.shoppingcart.ShoppingCartProto
import io.cloudstate.kotlinsupport.logger
import org.junit.Rule
import org.junit.Test
import org.testcontainers.containers.FixedHostPortGenericContainer
import org.testcontainers.containers.output.Slf4jLogConsumer
import org.testcontainers.containers.wait.strategy.Wait
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals

class IntegrationTests {
    private val log = logger()

    private val sys: ActorSystem = ActorSystem.create("GrpcTestClientSys")
    private val mat: Materializer = ActorMaterializer.create(sys)

    @Rule @JvmField
    val userFunction: FixedHostPortGenericContainer<*> = getUserFunction()

    @Test
    fun `Validate User Function Contract`() {
        log.info("Starting ShoppingCart Integration Test...")

        val userFunctionPort = userFunction.getFirstMappedPort().toString()
        log.debug("User Function Port -> $userFunctionPort")

        //We create here, without @Rule because we need to wait for the user's container to be created
        val proxy: FixedHostPortGenericContainer<*> = getProxy(userFunctionPort)
                .also(FixedHostPortGenericContainer<*>::start)

        log.info("Port bindings ${proxy.getPortBindings().size}")
        proxy.getPortBindings().forEach{ binding -> log.info("Port Binding -> $binding")}

        val clientSettings = GrpcClientSettings.connectToServiceAt("localhost", 9000, sys)
                .withTls(false)

        val client: ShoppingCartClient = ShoppingCartClient.create(clientSettings, mat, sys.dispatcher())

        client.addItem( addLineItem() )
                .toCompletableFuture()
                .get(30, TimeUnit.SECONDS)

        val cartItems: ShoppingCartProto.Cart = client.getCart( getCart() )
                .whenComplete { res, throwable ->
                    if (throwable == null) {
                        assertEquals(1, res.itemsCount)
                        assertEquals("1234587654", res.itemsList[0].productId)
                        assertEquals("TestProduct", res.itemsList[0].name)
                        assertEquals(1, res.itemsList[0].quantity)
                    } else {
                        log.error("Error on getCart", throwable)
                    }
                }.toCompletableFuture()
                .get(50, TimeUnit.SECONDS)

        log.info("Cart items -> $cartItems")

    }

    private fun addLineItem(): ShoppingCartProto.AddLineItem? =
            ShoppingCartProto.AddLineItem.newBuilder()
                    .setUserId("Adriano Santos Sleipnir")
                    .setName("TestProduct")
                    .setProductId("1234587654")
                    .setQuantity(1)
                    .build()

    private fun getCart(): ShoppingCartProto.GetShoppingCart? =
            ShoppingCartProto.GetShoppingCart.newBuilder()
                    .setUserId("Adriano Santos Sleipnir")
                    .build()

    private fun getUserFunction(): FixedHostPortGenericContainer<*> =
            FixedHostPortGenericContainer<Nothing>("sleipnir/shopping-cart:latest")
                    .apply{
                        withExposedPorts(8080)
                        withLogConsumer(Slf4jLogConsumer(log))
                        waitingFor(
                                Wait.forLogMessage(".*Successfully bound to .*", 1)
                        )
                    }

    private fun getProxy(userFunctionPort: String): FixedHostPortGenericContainer<*> =
            FixedHostPortGenericContainer<Nothing>("cloudstateio/cloudstate-proxy-native-dev-mode:latest")
                    .apply {
                        withNetworkMode("host")
                        //withExposedPorts(9000)
                        withFixedExposedPort(9000, 9000)
                        withEnv("USER_FUNCTION_PORT", userFunctionPort)
                        withLogConsumer(Slf4jLogConsumer(log))
                        waitingFor(
                                Wait.forLogMessage(".*CloudState proxy online.*", 1)
                        )
                    }

}
