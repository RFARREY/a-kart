package com.frogdesign.akart

import android.content.Context
import android.util.Log
import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONObject
import rx.Observable
import rx.subjects.BehaviorSubject
import timber.log.Timber
//import rx.lang.kotlin.BehaviourSubject
import java.security.cert.X509Certificate
import java.util.*
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext

class Comm(val id: String, val ctx: Context, val uri: String = Comm.DEFAULT_SERVER) {

    companion object {
        private val TAG = Comm::class.java.simpleName
        private val TRACE = true
        private val DEFAULT_SERVER = "https://a-kart.cloud.frogdesign.com/"
    }


    private val client: Socket
    val subject = BehaviorSubject.create<Event>()
    val pin: Observable<Event>

    init {
        // default SSLContext for all sockets
        //IO.setDefaultSSLContext(createSSLContext())
        //IO.setDefaultHostnameVerifier({ hostname, session -> true })
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
        }).on(Socket.EVENT_CONNECT_ERROR, { args ->
            trace("error" + Arrays.toString(args))
        }).on(Socket.EVENT_ERROR, { args ->
            trace("error" + Arrays.toString(args))
        }).on("set game", { args ->
            trace("set game" + args[0].javaClass.name)
            subject.onNext(GameState(args[0].toString().toBoolean()))
        }).on("hit", { args ->
            trace("hit")
            subject.onNext(Hit())
        }).on("speed", { args ->
            trace("speed")
            val percent = args[0].toString().toInt() / 100f
            subject.onNext(Speed(percent))
        }).on(Socket.EVENT_DISCONNECT, { args ->
            trace("disconnect")
        });
    }

    fun connect() {
        client.connect();
    }


    private fun createSSLContext(): SSLContext {
        var trustAllCerts = arrayOf(
                object : javax.net.ssl.X509TrustManager {
                    override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
                    }

                    override fun getAcceptedIssuers(): Array<out X509Certificate>? {
                        return null
                    }

                    override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {
                    }
                }
        )
        var sc = SSLContext.getInstance("SSL");
        sc.init(null, trustAllCerts, java.security.SecureRandom());
        return sc;
    }

    public fun close() {
        client.off()
        client.close()
    }

    public fun boom(id: String) {
        var obj = JSONObject()
        obj.put("id", id)
        client.emit("boom", obj)
    }

    public open class Event(val type: String)
    public class Message(val mex: String) : Event("message")
    public class Hit() : Event("hit")
    public class Speed(val percent: Float) : Event("hit")
    public class GameState(val on: Boolean) : Event("gamestate")
    public class Erroz(ex: Exception) : Event("error")

    private fun trace(s: String, vararg args: Any?) {
        if (TRACE) Timber.d(TAG, if (args != null) s.format(args) else s)
    }

    public fun send(s: String) {
        client.send(s)
    }
}