# kotlin-support

User Language Support for the Kotlin Programming Language

## EventSourcing example of use

### Define your proto

```proto
syntax = "proto3";

package com.example.shoppingcart;

import "google/protobuf/empty.proto";
import "cloudstate/entity_key.proto";
import "cloudstate/eventing.proto";
import "google/api/annotations.proto";
import "google/api/http.proto";
import "google/api/httpbody.proto";

message AddLineItem {
    string user_id = 1 [(.cloudstate.entity_key) = true];
    string product_id = 2;
    string name = 3;
    int32 quantity = 4;
}

message RemoveLineItem {
    string user_id = 1 [(.cloudstate.entity_key) = true];
    string product_id = 2;
}

message GetShoppingCart {
    string user_id = 1 [(.cloudstate.entity_key) = true];
}

message LineItem {
    string product_id = 1;
    string name = 2;
    int32 quantity = 3;
}

message Cart {
    repeated LineItem items = 1;
}

service ShoppingCart {
    rpc AddItem(AddLineItem) returns (google.protobuf.Empty) {
        option (google.api.http) = {
            post: "/cart/{user_id}/items/add",
            body: "*",
        };
        option (.cloudstate.eventing).in = "items";
    }

    rpc RemoveItem(RemoveLineItem) returns (google.protobuf.Empty) {
        option (google.api.http).post = "/cart/{user_id}/items/{product_id}/remove";
    }

    rpc GetCart(GetShoppingCart) returns (Cart) {
        option (google.api.http) = {
          get: "/carts/{user_id}",
          additional_bindings: {
            get: "/carts/{user_id}/items",
            response_body: "items"
          }
        };
    }
}

```

### Write your business logic
 
```kotlin
package com.example.shoppingcart

import com.example.shoppingcart.Shoppingcart
import com.example.shoppingcart.persistence.Domain
import com.google.protobuf.Empty
import io.cloudstate.javasupport.EntityId
import io.cloudstate.javasupport.eventsourced.CommandContext
import io.cloudstate.kotlinsupport.api.eventsourced.*

import java.util.stream.Collectors

@EventSourcedEntity
class ShoppingCartEntity(@param:EntityId private val entityId: String) {
    private val cart: MutableMap<String, Shoppingcart.LineItem?> = mutableMapOf<String, Shoppingcart.LineItem?>()

    @Snapshot
    fun snapshot(): Domain.Cart =
            Domain.Cart.newBuilder()
                    .addAllItems(
                            cart.values.stream()
                                    .map { item: Shoppingcart.LineItem? -> this.convert(item) }
                                    .collect(Collectors.toList())
                    )
                    .build()

    @SnapshotHandler
    fun handleSnapshot(cart: Domain.Cart) {
        this.cart.clear()
        for (item in cart.itemsList) {
            this.cart[item.productId] = convert(item)
        }
    }

    @EventHandler
    fun itemAdded(itemAdded: Domain.ItemAdded) {
        var item = cart[itemAdded.item.productId]

        item = if (item == null) {
            convert(itemAdded.item)
        } else {
            item.toBuilder()
                    .setQuantity(item.quantity + itemAdded.item.quantity)
                    .build()
        }
        cart[item!!.productId] = item
    }

    @EventHandler
    fun itemRemoved(itemRemoved: Domain.ItemRemoved): Shoppingcart.LineItem? = cart.remove(itemRemoved.productId)

    @CommandHandler
    fun getCart(): Shoppingcart.Cart = Shoppingcart.Cart.newBuilder().addAllItems(cart.values).build()

    @CommandHandler
    fun addItem(item: Shoppingcart.AddLineItem, ctx: CommandContext): Empty {
        if (item.quantity <= 0) {
            ctx.fail("Cannot add negative quantity of to item ${item.productId}" )
        }
        ctx.emit(
                Domain.ItemAdded.newBuilder()
                        .setItem(
                                Domain.LineItem.newBuilder()
                                        .setProductId(item.productId)
                                        .setName(item.name)
                                        .setQuantity(item.quantity)
                                        .build())
                        .build())
        return Empty.getDefaultInstance()
    }

    @CommandHandler
    fun removeItem(item: Shoppingcart.RemoveLineItem, ctx: CommandContext): Empty {
        if (!cart.containsKey(item.productId)) {
            ctx.fail("Cannot remove item ${item.productId} because it is not in the cart.")
        }
        ctx.emit(
                Domain.ItemRemoved.newBuilder()
                        .setProductId(item.productId)
                        .build())
        return Empty.getDefaultInstance()
    }

    private fun convert(item: Domain.LineItem): Shoppingcart.LineItem =
            Shoppingcart.LineItem.newBuilder()
                    .setProductId(item.productId)
                    .setName(item.name)
                    .setQuantity(item.quantity)
                    .build()


    private fun convert(item: Shoppingcart.LineItem?): Domain.LineItem =
            Domain.LineItem.newBuilder()
                    .setProductId(item!!.productId)
                    .setName(item.name)
                    .setQuantity(item.quantity)
                    .build()

}
```

### Register your Entity
```
package com.example.shoppingcart

import com.example.shoppingcart.persistence.Domain
import io.cloudstate.kotlinsupport.cloudstate

fun main() {
    cloudstate {
        // Options
        host = "0.0.0.0"
        port = 8088
        loglevel = "INFO"

        // Registered Entity
        registerEventSourcedEntity {
            entityService = ShoppingCartEntity::class
            descriptor = Shoppingcart.getDescriptor().findServiceByName("ShoppingCart")
            additionalDescriptors = arrayOf( Domain.getDescriptor() )
        }
    }.start()
            .toCompletableFuture()
            .get()
}

```