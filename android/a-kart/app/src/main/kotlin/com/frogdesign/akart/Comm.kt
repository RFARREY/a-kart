package com.frogdesign.akart

//import rx.lang.kotlin.BehaviourSubject
import android.content.Context
import com.frogdesign.akart.model.BoxFace
import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONObject
import rx.Observable
import rx.subjects.BehaviorSubject
import timber.log.Timber
import java.security.cert.X509Certificate
import java.util.*
import javax.net.ssl.SSLContext

class Comm(val id: String, val ctx: Context, val uri: String = Comm.DEFAULT_SERVER) {

    companion object {
        private val TAG = Comm::class.java.simpleName
        private val TRACE = true
        //private val DEFAULT_SERVER = "http://10.228.81.8:5000/"
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
            //client.disconnect()
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
            val percent = args[0].toString().toInt() / 100f
            trace("speed " + Arrays.toString(args) + ", percent" + percent)
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

    fun close() {
        client.off()
        client.close()
    }

    fun boom(id: String) {
        var obj = JSONObject()
        obj.put("id", id)
        client.emit("boom", obj)
    }

    open class Event(val type: String)
    class Message(val mex: String) : Event("message")
    class Hit() : Event("hit")
    class Speed(val percent: Float) : Event("speed")
    class GameState(val on: Boolean) : Event("gamestate")
    class Erroz(ex: Exception) : Event("error")

    private fun trace(s: String, vararg args: Any?) {
        if (TRACE) {
            Timber.d(s, args)
        }
    }

    fun send(s: String) {
        client.send(s)
    }

    fun bonus(boxFace: BoxFace) {

        Timber.i("BoxHit from %s for: %s", id, boxFace.markerValue)
        var obj = JSONObject()
        obj.put("player", id)
        obj.put("marker", boxFace.markerValue)
        client.emit("bonus", obj)
    }
}