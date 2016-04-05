package com.frogdesign.akart

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.RecyclerView.AdapterDataObserver
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import butterknife.bindView
import com.frogdesign.akart.model.Car
import com.frogdesign.akart.model.Cars
import com.frogdesign.akart.view.CheckableImageView
import com.parrot.arsdk.ardiscovery.ARDiscoveryDeviceService
import org.lucasr.twowayview.ItemSelectionSupport
import rx.Observable
import rx.Subscription
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import rx.subjects.PublishSubject
import timber.log.Timber
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper
import java.util.*
import kotlin.properties.Delegates

class DiscoveryActivity : AppCompatActivity() {
    private val list: RecyclerView by bindView(android.R.id.list)
    private val swiperefresh: SwipeRefreshLayout by bindView(R.id.swiperefresh)
    private val search: Button by bindView(R.id.search)
    private val prompt: TextView by bindView(R.id.prompt)

    private var adapter: DiscoveryAdapter by Delegates.notNull()
    private var discovery: Discovery by Delegates.notNull()
    private var selections: ItemSelectionSupport by Delegates.notNull()
    private var sub: Subscription? = null

    private val handler = Handler()
    private val stopRefresh = Runnable { swiperefresh.isRefreshing = false }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.discovery_activity)
        discovery = Discovery(baseContext)
        selections = ItemSelectionSupport.addTo(list)
        adapter = DiscoveryAdapter(this)

        list.layoutManager = LinearLayoutManager(this)
        list.setHasFixedSize(true)
        selections.choiceMode = ItemSelectionSupport.ChoiceMode.MULTIPLE
        list.adapter = adapter

        adapter.registerAdapterDataObserver(object : AdapterDataObserver() {
            override fun onChanged() {
                updateButton()
            }
        })

        search.setOnClickListener {
            val i = Intent(baseContext, PlayActivity::class.java)
            val checks = selections.checkedItemPositions
            if (checks.size() != 1)
                throw IllegalArgumentException("Should be 1 checked!")

            val checked = checks.keyAt(0)
            val car = adapter.getItemAt(checked)
            i.putExtra(PlayActivity.EXTRA_DEVICE, car.associatedDevice)
            startActivity(i)
        }
        swiperefresh.setOnRefreshListener {
            startDiscovery()
            selections.clearChoices()
            adapter.update(emptyList())
        }

        swiperefresh.setColorSchemeResources(R.color.militaryGreen, R.color.militaryGreen, R.color.militaryGreenDark)
        swiperefresh.setProgressBackgroundColorSchemeColor(Color.TRANSPARENT)
    }

    override fun attachBaseContext(newBase : Context) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }

    override fun onStop() {
        super.onStop()
        ensureUnsubscribed()
        discovery.unbind()
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
        sub = discovery.discoverer().subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread()).subscribe({ deviceList ->
            Timber.tag(TAG).i("--> SERVICES:")
            // Do what you want with the device list
            for (service in deviceList) {
                Timber.tag(TAG).i( "The service " + service)
            }
            Timber.tag(TAG).i( "<-- SERVICES.")

            @Suppress("UNCHECKED_CAST")
//            val dl = listOf(ARDiscoveryDeviceService("gargamella", null, 0),
//                                ARDiscoveryDeviceService("taxiguerrilla", null, 1))
            adapter.update(deviceList.map { dev -> Cars.retrieveRelatedTo(dev) }.filter { it -> it != null } as List<Car>)

        })

        swiperefresh.isRefreshing = true
        handler.removeCallbacks(stopRefresh)
        handler.postDelayed(stopRefresh, 3000);
    }

    override fun onResume() {
        super.onResume()
        updateButton()
    }

    private fun updateButton() {
        val size = selections.checkedItemPositions.size()
        search.isEnabled = size > 0

        val elems = adapter.itemCount
        prompt.visibility = if (elems > 0) View.INVISIBLE else View.VISIBLE
    }

    inner class DiscoveryAdapter constructor(ctx: Context) : RecyclerView.Adapter<DiscoveryListItemHolder>(), View.OnClickListener {

        private val layoutInflater: LayoutInflater
        private val devices = ArrayList<Car>()

        init {
            layoutInflater = LayoutInflater.from(ctx)
            setHasStableIds(true)
        }

        fun update(newDevices: List<Car>) {
            devices.clear()
            devices.addAll(newDevices)
            notifyDataSetChanged()
        }

        override fun getItemId(position: Int): Long {
            return devices[position].id.hashCode().toLong()
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

        override fun onClick(v: View) {
            val indexClicked = list.getChildAdapterPosition(v)
            val currentlyChecked = selections.isItemChecked(indexClicked)
            selections.clearChoices()
            Timber.i("Clicked on %s, currently %s", indexClicked, true)
            if (currentlyChecked) selections.setItemChecked(indexClicked, currentlyChecked)
            notifyDataSetChanged()
        }

        fun getItemAt(checked: Int): Car = devices[checked]
    }

    inner class DiscoveryListItemHolder constructor(v: View, private val on: View.OnClickListener) : RecyclerView.ViewHolder(v) {
        private val selectable: CheckableImageView
        private val avatar: ImageView
        private val text1: TextView

        init {
            selectable = v.findViewById(R.id.selectable) as CheckableImageView
            avatar = v.findViewById(R.id.avatar) as ImageView
            text1 = v.findViewById(android.R.id.text1) as TextView
        }

        fun bind(device: Car) {
            itemView.tag = device
            text1.text = device.id
            itemView.setOnClickListener(on)
            Timber.i("BIND index %s on %s", adapterPosition, selections.isItemChecked(adapterPosition))
            selectable.isChecked = selections.isItemChecked(adapterPosition) ?: false
        }
    }

    companion object {
        private val TAG = DiscoveryActivity::class.java.simpleName
    }
}
