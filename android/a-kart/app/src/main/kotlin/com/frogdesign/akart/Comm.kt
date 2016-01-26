package com.frogdesign.akart

import android.content.Context
import android.util.Log
import io.socket.client.IO
import io.socket.client.Socket
import rx.Observable
import rx.lang.kotlin.BehaviourSubject

class Comm(val id: String, val ctx: Context, val uri: String) {

    companion object {
        private val TAG = Comm::class.java.simpleName
        private val TRACE = true
    }


    private val client: Socket
    public val subject = BehaviourSubject<Event>()
    public val pin : Observable<Event>

    init {
        client = IO.socket(uri)
        trace("init!")
        pin = subject.asObservable()
        pin.doOnSubscribe {
            trace("onSub")
            client.disconnect()
        }

    }

    public fun connect() {
        client.on(Socket.EVENT_CONNECT, { args ->
            trace("connect, registering")
            client.emit("register", id)
        }).on("set game", { args ->
            trace("set game"+ args[0])
        }).on("start", { args ->
            trace("start")
        }).on("boom", { args ->
            trace("boom")
            subject.onNext(Hit())
        }).on(Socket.EVENT_DISCONNECT, { args ->
            trace("disconnect")
        });
        client.connect();
    }

    public fun disconnect() {
        client.disconnect()
    }

    public open class Event(val type: String)
    public class Message(val mex: String) : Event("message")
    public class Hit() : Event("hit")
    public class Erroz(ex: Exception) : Event("error")

    private fun trace(s: String, vararg args: Any?) {
        if (TRACE) Log.d(TAG, if (args != null) s.format(args) else s)
    }

    public fun send(s: String) {
        client.send(s)
    }
}