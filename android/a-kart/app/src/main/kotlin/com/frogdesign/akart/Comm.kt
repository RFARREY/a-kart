package com.frogdesign.akart

import android.content.Context
import android.util.Log
import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONObject
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

        client.on(Socket.EVENT_CONNECT, { args ->
            trace("connect, registering")
            client.emit("register", id)
        }).on("set game", { args ->
            trace("set game"+ args[0].javaClass.name)
            subject.onNext(GameState(args[0].toString().toBoolean()))
        }).on("hit", { args ->
            trace("hit")
            subject.onNext(Hit())
        }).on(Socket.EVENT_DISCONNECT, { args ->
            trace("disconnect")
        });
    }

    public fun connect() {
        client.connect();
    }

    public fun close() {
        client.off()
        client.close()
    }

    public fun boom(id : String) {
        var obj = JSONObject()
        obj.put("id", id)
        client.emit("boom", obj)
    }

    public open class Event(val type: String)
    public class Message(val mex: String) : Event("message")
    public class Hit() : Event("hit")
    public class GameState(val on : Boolean) : Event("gamestate")
    public class Erroz(ex: Exception) : Event("error")

    private fun trace(s: String, vararg args: Any?) {
        if (TRACE) Log.d(TAG, if (args != null) s.format(args) else s)
    }

    public fun send(s: String) {
        client.send(s)
    }
}