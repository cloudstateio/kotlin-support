package com.example.shoppingcart

// tag::shopping-cart-main[]
import ShoppingCartEntity
import io.cloudstate.kotlinsupport.cloudstate
import com.example.shoppingcart.persistence.Domain

fun main() {
    cloudstate {
        eventsourced {
            entityService = ShoppingCartEntity::class
            descriptor = Shoppingcart.getDescriptor().findServiceByName("ShoppingCart")
            additionalDescriptors = mutableListOf(Shoppingcart.getDescriptor(), Domain.getDescriptor() )
        }
    }.start()
            .toCompletableFuture()
            .get()
}
// end::shopping-cart-main[]
