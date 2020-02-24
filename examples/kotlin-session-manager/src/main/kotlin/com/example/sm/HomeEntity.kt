package com.example.sm

import com.example.sm.persistence.Domain
import com.google.protobuf.Empty
import io.cloudstate.javasupport.EntityId
import io.cloudstate.javasupport.eventsourced.*
import java.time.Instant
import java.util.*
import java.util.stream.Collectors

/** An event sourced entity.  */
@EventSourcedEntity(persistenceId = "home-account", snapshotEvery = 10)
class HomeEntity(@param:EntityId private val entityId: String) {

    private val sessions: MutableMap<String, Sm.Session?> = LinkedHashMap<String, Sm.Session?>()

    private var maxActiveSessions = MAX_ACTIVE_SESSIONS

    private fun convertToSessionResponse(session: Domain.Session): Sm.SessionResponse {
        return Sm.SessionResponse.newBuilder()
                .setAccountId(entityId)
                .setSessionId(session.sessionId)
                .setExpiration(session.expiration)
                .build()
    }

    private fun convert(session: Domain.Session): Sm.Session {
        return Sm.Session.newBuilder()
                .setDeviceId(session.deviceId)
                .setSessionId(session.sessionId)
                .setExpiration(session.expiration)
                .build()
    }

    private fun convert(session: Sm.Session): Domain.Session {
        return Domain.Session.newBuilder()
                .setDeviceId(session.deviceId)
                .setSessionId(session.sessionId)
                .setExpiration(session.expiration)
                .build()
    }

    private fun generateSessionID(): String {
        return UUID.randomUUID().toString()
    }

    private fun newSessionExpiration(): Long {
        return Instant.now().plusSeconds(SESSION_DURATION_IN_SECONDS.toLong()).epochSecond
    }

    private fun hasExpired(s: Sm.Session?): Boolean {
        val now = Instant.now().epochSecond
        if (s != null) {
            return now >= s.expiration
        }
        return false

    }

    private fun purgeExpiredSessions(ctx: CommandContext) {
        val expiredSessions = Vector<String>()
        for (s in sessions.values) {
            if (hasExpired(s)) {
                if (s != null) {
                    expiredSessions.add(s.sessionId)
                }
            }
        }
        for (sessionId in expiredSessions) {
            ctx.emit(Domain.SessionExpired.newBuilder().setSessionId(sessionId).build())
        }
    }

    @CommandHandler
    fun setMaxSession(max: Sm.MaxSession, ctx: CommandContext): Empty {
        ctx.emit(
                Domain.MaxSessionSet.newBuilder().setNbActiveSessions(max.maxNbOfSession).build())
        return Empty.getDefaultInstance()
    }

    @EventHandler
    fun maxSessionSet(evt: Domain.MaxSessionSet) {
        maxActiveSessions = evt.nbActiveSessions
    }

    @CommandHandler
    fun getHome(ctx: CommandContext): Sm.Home {
        purgeExpiredSessions(ctx)
        return Sm.Home.newBuilder()
                .addAllSessions(sessions.values)
                .setNbActiveSessions(maxActiveSessions)
                .build()
    }

    @CommandHandler
    fun createSession(sessionSetup: Sm.SessionSetup, ctx: CommandContext): Sm.SessionResponse {
        purgeExpiredSessions(ctx)
        // we should not only verify the max but also purge expired sessions
        if (sessions.size >= maxActiveSessions) {
            ctx.fail(
                    "Cannot create session. Max of active sessions ($maxActiveSessions) exhausted!")
        }
        val newSessionID = generateSessionID()
        val createdSession: Domain.Session = Domain.Session.newBuilder()
                .setDeviceId(sessionSetup.deviceId)
                .setSessionId(newSessionID)
                .setExpiration(newSessionExpiration())
                .build()
        ctx.emit(Domain.SessionCreated.newBuilder().setSession(createdSession).build())
        return convertToSessionResponse(createdSession)
    }

    @EventHandler
    fun sessionCreated(sessionCreated: Domain.SessionCreated) {
        val session: Sm.Session = convert(sessionCreated.session)
        sessions[session.sessionId] = session
    }

    @CommandHandler
    fun heartBeat(session: Sm.HeartBeatSession, ctx: CommandContext): Sm.SessionResponse { // In case of heartbeat, the SM renew always the session. It will not check the max.
        val s: Sm.Session? = sessions[session.sessionId]
        if (s == null) {
            ctx.fail("Cannot renew non-existing session!")
        } else if (hasExpired(s)) {
            ctx.emit(Domain.SessionExpired.newBuilder().setSessionId(session.sessionId).build())
            ctx.fail("Cannot renew expired session!")
        }
        val newSessionID = generateSessionID()
        val createdSession: Domain.Session = Domain.Session.newBuilder()
                .setDeviceId(s?.deviceId)
                .setSessionId(newSessionID)
                .setExpiration(newSessionExpiration())
                .build()
        ctx.emit(
                Domain.SessionRenewed.newBuilder()
                        .setNewSession(createdSession)
                        .setExpiredSessionId(session.sessionId)
                        .build())
        return convertToSessionResponse(createdSession)
    }

    @EventHandler
    fun sessionRenewed(sessionRenewed: Domain.SessionRenewed) {
        val expiredSessionId: String = sessionRenewed.expiredSessionId
        var session: Sm.Session? = sessions[expiredSessionId]
        // Null session should never happen because before a renew, we should always already a session
        if (session != null) {
            sessions.remove(expiredSessionId)
        }
        session = convert(sessionRenewed.newSession)
        sessions[session.sessionId] = session
    }

    @CommandHandler
    fun tearDown(session: Sm.TearDownSession, ctx: CommandContext): Empty {
        ctx.emit(Domain.SessionTearedDown.newBuilder().setSessionId(session.sessionId).build())
        return Empty.getDefaultInstance()
    }

    @EventHandler
    fun tearDown(sessionTearedDown: Domain.SessionTearedDown) {
        val sessionId: String = sessionTearedDown.sessionId
        val session: Sm.Session? = sessions[sessionId]
        // Null session should never happen because before a tearDown, session exists
        if (session != null) {
            sessions.remove(sessionId)
        }
    }

    @EventHandler
    fun sessionExpired(sessionExpired: Domain.SessionExpired) {
        sessions.remove(sessionExpired.sessionId)
    }

    @Snapshot
    fun snapshot(): Domain.Home {
        val sessions = sessions.values.stream()
                .map {
                    return@map it?.let { it1 -> convert(it1) }
                }.collect(Collectors.toList())

        return Domain.Home.newBuilder()
                .addAllSessions(sessions)
                .setNbActiveSessions(maxActiveSessions)
                .build()
    }

    @SnapshotHandler
    fun handleSnapshot(home: Domain.Home) {
        sessions.clear()
        for (session in home.getSessionsList()) {
            sessions[session.getSessionId()] = convert(session)
        }
        maxActiveSessions = home.getNbActiveSessions()
    }

    companion object {
        private const val MAX_ACTIVE_SESSIONS = 2
        private const val SESSION_DURATION_IN_SECONDS = 2 * 60
    }

}