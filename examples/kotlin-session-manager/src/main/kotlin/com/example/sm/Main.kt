package com.example.sm

import com.example.smdevice.Smdevice

import io.cloudstate.kotlinsupport.cloudstate

fun main(args: Array<String>) {
    cloudstate {
        registerEventSourcedEntity {
            entityService = DeviceEntity::class.java
            descriptor = Smdevice.getDescriptor().findServiceByName("Device")
            additionalDescriptors = arrayOf( com.example.smdevice.persistence.Domain.getDescriptor() )
        }

        registerEventSourcedEntity {
            entityService = HomeEntity::class.java
            descriptor = Sm.getDescriptor().findServiceByName("SM")
            additionalDescriptors = arrayOf( com.example.sm.persistence.Domain.getDescriptor() )
        }
    }.start()
            .toCompletableFuture()
            .get()
}
