package us.koller.cameraroll.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;

import java.util.ArrayList;

import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

//simple BaseActivity that handles LocalBroadcastReceivers, need for communication with FileOperationServices
public abstract class BaseActivity extends AppCompatActivity {

    private ArrayList<BroadcastReceiver> broadcastReceivers;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        broadcastReceivers = new ArrayList<>();

        BroadcastReceiver defaultBroadcastReceiver = getDefaultLocalBroadcastReceiver();
        if (defaultBroadcastReceiver != null) {
            registerLocalBroadcastReceiver(defaultBroadcastReceiver);
        }
    }

    //for RobotoMono font
    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }

    @Override
    protected void onDestroy() {
        //unregister LocalBroadcastReceivers
        for (int i = 0; i < broadcastReceivers.size(); i++) {
            BroadcastReceiver broadcastReceiver = broadcastReceivers.get(i);
            if (broadcastReceiver != null) {
                unregisterLocalBroadcastReceiver(broadcastReceiver);
            }
        }

        super.onDestroy();
    }

    public BroadcastReceiver getDefaultLocalBroadcastReceiver() {
        return null;
    }

    public void registerLocalBroadcastReceiver(BroadcastReceiver broadcastReceiver) {
        broadcastReceivers.add(broadcastReceiver);
        LocalBroadcastManager.getInstance(this)
                .registerReceiver(broadcastReceiver, getBroadcastIntentFilter());
    }

    public void unregisterLocalBroadcastReceiver(BroadcastReceiver broadcastReceiver) {
        broadcastReceivers.remove(broadcastReceiver);
        LocalBroadcastManager.getInstance(this)
                .unregisterReceiver(broadcastReceiver);
    }

    public IntentFilter getBroadcastIntentFilter() {
        return new IntentFilter();
    }
}
