package com.frogdesign.akart

import android.content.Context
import android.util.Log
import org.java_websocket.WebSocket
import org.java_websocket.client.WebSocketClient
import org.java_websocket.drafts.Draft_17
import org.java_websocket.handshake.ServerHandshake
import rx.lang.kotlin.BehaviourSubject
import java.net.URI

class Comm(ctx: Context, uri: String) {

    companion object {
        private val TAG = Comm::class.java.simpleName
        private val TRACE = false

        private var inst: Comm? = null;

        fun instance(ctx: Context): Comm = synchronized(Comm, {
            if (inst == null) inst = Comm(ctx, "")
            return inst!!
        })
    }

    private class WSClient : WebSocketClient {

        private val ctx: Context
        private val comm: Comm

        constructor(ctx: Context, comm: Comm, uri: URI?) : super(uri, Draft_17()) {
            this.ctx = ctx
            this.comm = comm
        }

        override fun onClose(code: Int, reason: String?, remote: Boolean) {
            comm.trace("onClose %d %s %b", code, reason, remote)
        }

        override fun onError(ex: Exception?) {
            comm.trace("onError %s %b", ex)
            comm.subject.onNext(Erroz(ex!!))
        }

        override fun onMessage(message: String?) {
            comm.trace("onMessage %s", message)
            comm.subject.onNext(Message(message!!))
        }

        override fun onOpen(handshakedata: ServerHandshake?) {
            comm.trace("onOpen %s", handshakedata)
        }
    }

    private val client: WSClient
    public val subject = BehaviourSubject<Event>()

    init {
        client = WSClient(ctx, this, URI.create(uri))
        subject.doOnSubscribe({
            if (client.readyState != WebSocket.READYSTATE.OPEN &&
                    client.readyState != WebSocket.READYSTATE.CONNECTING)
                client.connect()
        })
        subject.doOnUnsubscribe({
            if (!subject.hasObservers()) client.close()
        }
        )
    }

    public open class Event(val type: String)
    public class Message(val mex: String) : Event("message")
    public class Erroz(ex: Exception) : Event("error")

    private fun trace(s: String, vararg args: Any?) {
        if (TRACE) Log.d(TAG, s.format(args))
    }

    public fun send(s : String) {
        if (client.readyState == WebSocket.READYSTATE.OPEN) {
            client.send(s)
        }
    }
}