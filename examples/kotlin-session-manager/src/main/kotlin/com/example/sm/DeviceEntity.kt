package com.example.sm

import com.example.smdevice.Smdevice
import com.example.smdevice.persistence.Domain
import com.google.protobuf.Empty
import io.cloudstate.javasupport.Context
import io.cloudstate.javasupport.EntityId
import io.cloudstate.javasupport.ServiceCallRef
import io.cloudstate.javasupport.eventsourced.*
import java.util.*

@EventSourcedEntity
class DeviceEntity(@param:EntityId private val entityId: String, ctx: Context) {

    private val sessionCreationRef: ServiceCallRef<Sm.SessionSetup> = ctx.serviceCallFactory()
            .lookup("com.example.SM", "CreateSession", Sm.SessionSetup::class.java)

    private var device: Optional<Smdevice.DeviceInfo> = Optional.empty<Smdevice.DeviceInfo>()
    @Snapshot
    fun snapshot(): Optional<Domain.Device> {
        return if (device.isPresent()) {
            Optional.of(
                    Domain.Device.newBuilder().setAccountId(device.get().getAccountId()).build())
        } else {
            Optional.empty<Domain.Device>()
        }
    }

    @SnapshotHandler
    fun handleSnapshot(device: Domain.Device) {
        this.device = Optional.of(Smdevice.DeviceInfo.newBuilder().setAccountId(device.getAccountId()).build())
    }

    @EventHandler
    fun deviceCreated(deviceCreated: Domain.DeviceCreated) {
        device = Optional.of(
                Smdevice.DeviceInfo.newBuilder().setAccountId(deviceCreated.getAccountId()).build())
    }

    @EventHandler
    fun deviceDeleted(deviceDeleted: Domain.DeviceDeleted?) {
        device = Optional.empty<Smdevice.DeviceInfo>()
    }

    @CommandHandler
    fun createSessionWithDevice(
            sessionSetup: Smdevice.SessionSetupWithDevice, ctx: CommandContext) {
        if (!device.isPresent()) {
            ctx.fail("Device not registered")
        }
        val accountID: String = device.get().getAccountId()
        val call = sessionCreationRef.createCall(
                Sm.SessionSetup.newBuilder()
                        .setAccountId(accountID)
                        .setDeviceId(sessionSetup.getDeviceId())
                        .build())
        ctx.forward(call)
    }

    @CommandHandler
    fun getDevice(ctx: CommandContext): Smdevice.DeviceInfo { // Return a copy
        if (!device.isPresent()) {
            ctx.fail("Device has been deleted")
        }
        return Smdevice.DeviceInfo.newBuilder().setAccountId(device.get().getAccountId()).build()
    }

    @CommandHandler
    fun createDevice(param: Smdevice.CreateDeviceParam, ctx: CommandContext): Empty {
        ctx.emit(Domain.DeviceCreated.newBuilder().setAccountId(param.getAccountId()).build())
        return Empty.getDefaultInstance()
    }

    @CommandHandler
    fun deleteDevice(param: Smdevice.DeleteDeviceParam?, ctx: CommandContext): Empty {
        ctx.emit(Domain.DeviceDeleted.newBuilder().setAccountId(device.get().getAccountId()).build())
        return Empty.getDefaultInstance()
    }

}