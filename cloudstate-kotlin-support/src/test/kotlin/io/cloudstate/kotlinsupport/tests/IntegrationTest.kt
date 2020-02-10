package io.cloudstate.kotlinsupport.tests

import io.cloudstate.kotlinsupport.logger
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.Network

class IntegrationTest {

    private val log = logger()

    private val network: Network = Network.newNetwork()

    companion object {

        @BeforeClass
        @JvmStatic fun setup() {
            // things to execute once and keep around for the class
            /*cloudstate {

                registerEventSourcedEntity {
                    entityService = ShoppingCartEntity::class.java

                    descriptor = Shoppingcart.getDescriptor().findServiceByName("ShoppingCart")
                    additionalDescriptors = arrayOf( Domain.getDescriptor() )

                    snapshotEvery = 1
                    persistenceId = "shopping-cart"
                }
            }.start()
                    .toCompletableFuture()
                    .get()*/
        }

    }

    @Rule @JvmField
    val userFunction: GenericContainer<*> = GenericContainer<Nothing>("sleipnir/shopping-cart:latest")
            .apply{
                withExposedPorts(8080);
                withNetworkAliases("cloudstate");
            }

    @Rule @JvmField
    val proxy: GenericContainer<*> = GenericContainer<Nothing>("cloudstateio/cloudstate-proxy-native-dev-mode:latest")
            .apply{
                withExposedPorts(9000);
                withNetwork(network);
            }

    @Test fun shoppingCartIntegrationTest() {
        log.info("Starting shoppingCartIntegrationTest...")
        //Testcontainers.exposeHostPorts(8080);
    }


}
