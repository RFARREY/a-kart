package com.frogdesign.artest;

import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.frogdesign.arsdk.Discovery;
import com.frogdesign.arsdk.TestUtils;
import com.frogdesign.artest.databinding.DiscoveryListItemBinding;
import com.parrot.arsdk.ardiscovery.ARDiscoveryDeviceService;

import java.util.ArrayList;
import java.util.List;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;

public class DiscoveryActivity extends AppCompatActivity {
    private static final String TAG = DiscoveryActivity.class.getSimpleName();
    private RecyclerView list;
    private DiscoveryAdapter adapter;
    private LinearLayoutManager llm;
    private Discovery discovery;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DataBindingUtil.setContentView(this, R.layout.discovery_activity);
        discovery = new Discovery(getBaseContext());
        list = (RecyclerView) findViewById(android.R.id.list);
        list.setHasFixedSize(true);
        llm = new LinearLayoutManager(this);
        list.setLayoutManager(llm);
        adapter = new DiscoveryAdapter(this);
        list.setAdapter(adapter);

        adapter.clicksOn().subscribe(dev -> {
            Log.i("CLICK", "on " + dev.getName());
            Intent i = new Intent(getBaseContext(), MainActivity.class);
            startActivity(i);
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        discovery.discoverer();
        TestUtils.constantDiscoverer()
                .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(deviceList -> {
            Log.i(TAG, "--> SERVICES:");
            // Do what you want with the device list
            for (ARDiscoveryDeviceService service : deviceList) {
                Log.i(TAG, "The service " + service);
            }
            Log.i(TAG, "<-- SERVICES.");
            adapter.update(deviceList);
            //Controller ctrl = new Controller(getBaseContext(), deviceList.get(0));
            //ctrl.start();
            //ctrl.mediaStreamer().observeOn(AndroidSchedulers.mainThread()).subscribe(bmpConsumer);
        });
    }

    public static class DiscoveryAdapter extends RecyclerView.Adapter<DiscoveryListItemHolder>
            implements View.OnClickListener {

        private final LayoutInflater layoutInflater;
        private final PublishSubject<ARDiscoveryDeviceService> subject = PublishSubject.create();
        private final ArrayList<ARDiscoveryDeviceService> devices = new ArrayList<>();

        private DiscoveryAdapter(Context ctx) {
            layoutInflater = LayoutInflater.from(ctx);
            setHasStableIds(true);
        }

        private void update(List<ARDiscoveryDeviceService> newDevices) {
            devices.clear();
            devices.addAll(newDevices);
            notifyDataSetChanged();
        }

        @Override
        public long getItemId(int position) {
            return devices.get(position).getName().hashCode();
        }

        @Override
        public DiscoveryListItemHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            DiscoveryListItemBinding binding =
                    DiscoveryListItemBinding.inflate(layoutInflater, parent, false);
            binding.setHandler(this);
            return new DiscoveryListItemHolder(binding);
        }

        @Override
        public void onBindViewHolder(DiscoveryListItemHolder holder, int position) {
            holder.bind(new ARDiscoveryDeviceService("fake" + position, null, position));
        }

        @Override
        public int getItemCount() {
            return devices.size();
        }

        public Observable<ARDiscoveryDeviceService> clicksOn() {
            return subject;
        }

        @Override
        public void onClick(View v) {
            ARDiscoveryDeviceService device = (ARDiscoveryDeviceService) v.getTag();
            subject.onNext(device);
        }
    }

    public static class DiscoveryListItemHolder extends RecyclerView.ViewHolder {
        private DiscoveryListItemBinding binding;

        private DiscoveryListItemHolder(DiscoveryListItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        private void bind(ARDiscoveryDeviceService device) {
            itemView.setTag(device);
            binding.setDevice(device);
            binding.executePendingBindings();
        }
    }
}
