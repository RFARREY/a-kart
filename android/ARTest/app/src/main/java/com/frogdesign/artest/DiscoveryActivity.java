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

import com.frogdesign.artest.databinding.DiscoveryListItemBinding;
import com.parrot.arsdk.ardiscovery.ARDiscoveryDeviceService;

import rx.Observable;
import rx.subjects.PublishSubject;

public class DiscoveryActivity extends AppCompatActivity {

    private RecyclerView list;
    private DiscoveryAdapter adapter;
    private LinearLayoutManager llm;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DataBindingUtil.setContentView(this, R.layout.discovery_activity);
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

    public static class DiscoveryAdapter extends RecyclerView.Adapter<DiscoveryListItemHolder> implements View.OnClickListener {

        private LayoutInflater layoutInflater;
        private final PublishSubject<ARDiscoveryDeviceService> subject = PublishSubject.create();

        private DiscoveryAdapter(Context ctx) {
            layoutInflater = LayoutInflater.from(ctx);
            setHasStableIds(true);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public DiscoveryListItemHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            DiscoveryListItemBinding binding = DiscoveryListItemBinding.inflate(layoutInflater, parent, false);
            binding.setHandler(this);
            return new DiscoveryListItemHolder(binding);
        }

        @Override
        public void onBindViewHolder(DiscoveryListItemHolder holder, int position) {
            holder.bind(new ARDiscoveryDeviceService("fake" + position, null, position));
        }

        @Override
        public int getItemCount() {
            return 30;
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
