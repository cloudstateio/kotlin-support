package io.cloudstate.examples.pingpong

import com.google.protobuf.Empty
import io.cloudstate.javasupport.EntityId
import io.cloudstate.javasupport.eventsourced.*

@EventSourcedEntity
class PingPongEntity(@param:EntityId private val entityId: String) {

    private var sentPings = 0
    private var seenPings = 0
    private var sentPongs = 0
    private var seenPongs = 0

    @Snapshot
    fun snapshot(): Pingpong.PingPongStats =
            Pingpong.PingPongStats.newBuilder()
                .setSeenPongs(seenPongs)
                .setSeenPings(seenPings)
                .setSentPongs(sentPongs)
                .setSentPings(sentPings)
                .build()

    @SnapshotHandler
    fun handleSnapshot(stats: Pingpong.PingPongStats) {
        seenPings = stats.seenPings
        seenPongs = stats.seenPongs
        sentPings = stats.sentPings
        sentPongs = stats.sentPongs
    }

    @EventHandler
    fun pongSent(pong: Pingpong.PongSent?) {
        sentPongs += 1
    }

    @EventHandler
    fun pongSent(ping: Pingpong.PingSent?) {
        sentPings += 1
    }

    @EventHandler
    fun pongSent(ping: Pingpong.PingSeen?) {
        seenPings += 1
    }

    @EventHandler
    fun pongSent(pong: Pingpong.PongSeen?) {
        seenPongs += 1
    }

    @CommandHandler
    fun ping(pong: Pingpong.PongSent, ctx: CommandContext): Pingpong.PingSent {
        val sent: Pingpong.PingSent = Pingpong.PingSent.newBuilder()
                .setId(pong.id)
                .setSequenceNumber(pong.sequenceNumber + 1)
                .build()
        ctx.emit(sent)
        return sent
    }

    @CommandHandler
    fun pong(ping: Pingpong.PingSent, ctx: CommandContext): Pingpong.PongSent {
        val sent: Pingpong.PongSent = Pingpong.PongSent.newBuilder()
                .setId(ping.id)
                .setSequenceNumber(ping.sequenceNumber + 1)
                .build()
        ctx.emit(sent)
        return sent
    }

    @CommandHandler
    fun seenPong(pong: Pingpong.PongSent, ctx: CommandContext): Empty {
        ctx.emit(
                Pingpong.PingSeen.newBuilder()
                        .setId(pong.id)
                        .setSequenceNumber(pong.sequenceNumber)
                        .build())
        return Empty.getDefaultInstance()
    }

    @CommandHandler
    fun seenPing(ping: Pingpong.PingSent, ctx: CommandContext): Empty {
        ctx.emit(
                Pingpong.PingSeen.newBuilder()
                        .setId(ping.id)
                        .setSequenceNumber(ping.sequenceNumber)
                        .build())
        return Empty.getDefaultInstance()
    }

    @CommandHandler
    fun report(ping: Pingpong.GetReport?, ctx: CommandContext?): Pingpong.PingPongStats = snapshot()

}