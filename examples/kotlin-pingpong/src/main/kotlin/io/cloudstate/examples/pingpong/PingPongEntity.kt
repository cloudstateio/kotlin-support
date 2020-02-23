package io.cloudstate.examples.pingpong

import com.google.protobuf.Empty
import io.cloudstate.javasupport.EntityId
import io.cloudstate.javasupport.eventsourced.*
import io.cloudstate.pingpong.*

/** An event sourced entity.  */
@EventSourcedEntity
class PingPongEntity(@param:EntityId private val entityId: String) {
    private var sentPings = 0
    private var seenPings = 0
    private var sentPongs = 0
    private var seenPongs = 0
    @Snapshot
    fun snapshot(): Pingpong.PingPongStats {
        return Pingpong.PingPongStats.newBuilder()
                .setSeenPongs(seenPongs)
                .setSeenPings(seenPings)
                .setSentPongs(sentPongs)
                .setSentPings(sentPings)
                .build()
    }

    @SnapshotHandler
    fun handleSnapshot(stats: Pingpong.PingPongStats) {
        seenPings = stats.getSeenPings()
        seenPongs = stats.getSeenPongs()
        sentPings = stats.getSentPings()
        sentPongs = stats.getSentPongs()
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
                .setId(pong.getId())
                .setSequenceNumber(pong.getSequenceNumber() + 1)
                .build()
        ctx.emit(sent)
        return sent
    }

    @CommandHandler
    fun pong(ping: Pingpong.PingSent, ctx: CommandContext): Pingpong.PongSent {
        val sent: Pingpong.PongSent = Pingpong.PongSent.newBuilder()
                .setId(ping.getId())
                .setSequenceNumber(ping.getSequenceNumber() + 1)
                .build()
        ctx.emit(sent)
        return sent
    }

    @CommandHandler
    fun seenPong(pong: Pingpong.PongSent, ctx: CommandContext): Empty {
        ctx.emit(
                Pingpong.PingSeen.newBuilder()
                        .setId(pong.getId())
                        .setSequenceNumber(pong.getSequenceNumber())
                        .build())
        return Empty.getDefaultInstance()
    }

    @CommandHandler
    fun seenPing(ping: Pingpong.PingSent, ctx: CommandContext): Empty {
        ctx.emit(
                Pingpong.PingSeen.newBuilder()
                        .setId(ping.getId())
                        .setSequenceNumber(ping.getSequenceNumber())
                        .build())
        return Empty.getDefaultInstance()
    }

    @CommandHandler
    fun report(ping: Pingpong.GetReport?, ctx: CommandContext?): Pingpong.PingPongStats {
        return snapshot()
    }

}