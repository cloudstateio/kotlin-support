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
import java.util.concurrent.CompletionStage
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals

class IntegrationTest {
    private val log = logger()

    private val sys: ActorSystem = ActorSystem.create("GrpcTestClientSys")
    private val materializer: Materializer = ActorMaterializer.create(sys)

    @Rule @JvmField
    val userFunction: FixedHostPortGenericContainer<*> = FixedHostPortGenericContainer<Nothing>("sleipnir/shopping-cart:latest")
            .apply{
                withExposedPorts(8080)
                withLogConsumer(Slf4jLogConsumer(log))
                waitingFor(
                        Wait.forLogMessage(".*Successfully bound to .*", 1)
                )
            }

    @Test
    fun shoppingCartIntegrationTest() {
        log.info("Starting shoppingCartIntegrationTest...")

        val userFunctionPort = userFunction.getFirstMappedPort().toString()
        log.debug("User Funtion Port -> $userFunctionPort")

        val proxy: FixedHostPortGenericContainer<*> = getProxy(userFunctionPort).also(FixedHostPortGenericContainer<*>::start)

        log.info("Port bindings ${proxy.getPortBindings().size}")
        proxy.getPortBindings().forEach{ binding -> log.info("Port Binding -> $binding")}

        val clientSettings = GrpcClientSettings.connectToServiceAt("localhost", 9000, sys)
                .withTls(false)

        val client: ShoppingCartClient = ShoppingCartClient.create(clientSettings, materializer, sys.dispatcher())

        val cart = ShoppingCartProto.GetShoppingCart.newBuilder()
                .setUserId("Adriano Santos Sleipnir")
                .build()

        val addLineItem = ShoppingCartProto.AddLineItem.newBuilder()
                .setUserId("Adriano Santos Sleipnir")
                .setName("TestProduct")
                .setProductId("1234587654")
                .setQuantity(1)
                .build()

        client.addItem(addLineItem).toCompletableFuture()
                .get(30, TimeUnit.SECONDS)

        val getCartResponse: CompletionStage<ShoppingCartProto.Cart> = client.getCart(cart)

        val cartItems: ShoppingCartProto.Cart = getCartResponse.whenComplete { res, throwable ->
            if (throwable == null) {
                assertEquals(1, res.itemsCount)
                assertEquals("1234587654", res.itemsList[0].productId)
                assertEquals("TestProduct", res.itemsList[0].name)
                assertEquals(1, res.itemsList[0].quantity)
            } else {
                log.error("Error on getCart", throwable)
            }
        }.toCompletableFuture().get(50, TimeUnit.SECONDS)

        log.info("Cart items -> $cartItems")

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
