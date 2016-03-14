package com.frogdesign.akart

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import butterknife.bindView
import com.parrot.arsdk.ardiscovery.ARDiscoveryDeviceService
import rx.Observable
import rx.Subscription
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import rx.subjects.PublishSubject
import timber.log.Timber
import java.util.*

class DiscoveryActivity : AppCompatActivity() {
    private val list: RecyclerView by bindView(android.R.id.list)
    private val swiperefresh: SwipeRefreshLayout by bindView(R.id.swiperefresh)
    private val search: Button by bindView(R.id.search)

    private var adapter: DiscoveryAdapter? = null
    private var llm: LinearLayoutManager? = null
    private var discovery: Discovery? = null
    private var sub: Subscription? = null

    private val handler = Handler()
    private val stopRefresh = Runnable { swiperefresh.isRefreshing = false }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.discovery_activity)
        discovery = Discovery(baseContext)
        list.setHasFixedSize(true)
        llm = LinearLayoutManager(this)
        list.layoutManager = llm
        adapter = DiscoveryAdapter(this)
        list.adapter = adapter

        adapter!!.clicksOn().subscribe { dev ->
            val i = Intent(baseContext, PlayActivity::class.java)
            i.putExtra(PlayActivity.EXTRA_DEVICE, dev)
            startActivity(i)
        }

        search.setOnClickListener { startDiscovery() }
        swiperefresh.setOnRefreshListener { startDiscovery() }

        swiperefresh.setColorSchemeResources(R.color.colorAccent, R.color.colorPrimary, R.color.colorPrimaryDark)
    }

    override fun onStop() {
        super.onStop()
        ensureUnsubscribed()
        discovery?.unbind()
        stopRefresh.run()
        handler.removeCallbacks(stopRefresh)
    }

    private fun ensureUnsubscribed() {
        if (sub != null && !sub!!.isUnsubscribed) {
            sub!!.unsubscribe()
            sub = null
        }
    }

    private fun startDiscovery() {
        ensureUnsubscribed()
        sub = discovery!!.discoverer().subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread()).subscribe({ deviceList ->
            Timber.i(TAG, "--> SERVICES:")
            // Do what you want with the device list
            for (service in deviceList) {
                Timber.i(TAG, "The service " + service)
            }
            Timber.i(TAG, "<-- SERVICES.")
            adapter!!.update(deviceList)
        })

        swiperefresh.isRefreshing = true
        handler.removeCallbacks(stopRefresh)
        handler.postDelayed(stopRefresh, 3000);
    }

    class DiscoveryAdapter constructor(ctx: Context) : RecyclerView.Adapter<DiscoveryListItemHolder>(), View.OnClickListener {

        private val layoutInflater: LayoutInflater
        private val subject = PublishSubject.create<ARDiscoveryDeviceService>()
        private val devices = ArrayList<ARDiscoveryDeviceService>()

        init {
            layoutInflater = LayoutInflater.from(ctx)
            setHasStableIds(true)
        }

        fun update(newDevices: List<ARDiscoveryDeviceService>) {
            devices.clear()
            devices.addAll(newDevices)
            notifyDataSetChanged()
        }

        override fun getItemId(position: Int): Long {
            return devices[position].name.hashCode().toLong()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DiscoveryListItemHolder {
            val v = layoutInflater.inflate(R.layout.discovery_list_item, parent, false)
            return DiscoveryListItemHolder(v, this)
        }

        override fun onBindViewHolder(holder: DiscoveryListItemHolder, position: Int) {
            holder.bind(devices[position])
        }

        override fun getItemCount(): Int {
            return devices.size
        }

        fun clicksOn(): Observable<ARDiscoveryDeviceService> {
            return subject
        }

        override fun onClick(v: View) {
            val device = v.tag as ARDiscoveryDeviceService
            subject.onNext(device)
        }
    }

    class DiscoveryListItemHolder constructor(v: View, private val on: View.OnClickListener) : RecyclerView.ViewHolder(v) {
        private val text1: TextView

        init {
            text1 = v.findViewById(android.R.id.text1) as TextView
        }

        fun bind(device: ARDiscoveryDeviceService) {
            itemView.tag = device
            text1.text = device.name
            itemView.setOnClickListener(on)
        }
    }

    companion object {
        private val TAG = DiscoveryActivity::class.java.simpleName
    }
}
