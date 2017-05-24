package us.koller.cameraroll.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;

import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

public abstract class BaseActivity extends AppCompatActivity {

    private BroadcastReceiver broadcastReceiver;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //register LocalBroadcastReceiver
        BroadcastReceiver broadcastReceiver = getLocalBroadcastReceiver();
        if (broadcastReceiver != null) {
            LocalBroadcastManager.getInstance(this)
                    .registerReceiver(broadcastReceiver, getBroadcastIntentFilter());
        }
    }

    //for RobotoMono font
    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }

    @Override
    protected void onDestroy() {
        //unregister LocalBroadcastReceiver
        BroadcastReceiver broadcastReceiver = getLocalBroadcastReceiver();
        if (broadcastReceiver != null) {
            LocalBroadcastManager.getInstance(this)
                    .unregisterReceiver(broadcastReceiver);
        }
        setLocalBroadcastReceiver(null);

        super.onDestroy();
    }

    public BroadcastReceiver getLocalBroadcastReceiver() {
        return broadcastReceiver;
    }

    public void setLocalBroadcastReceiver(BroadcastReceiver broadcastReceiver) {
        this.broadcastReceiver = broadcastReceiver;
        //re-register new broadcastReceiver
        if (broadcastReceiver != null) {
            LocalBroadcastManager.getInstance(this)
                    .registerReceiver(broadcastReceiver, getBroadcastIntentFilter());
        }
    }

    public IntentFilter getBroadcastIntentFilter() {
        return new IntentFilter();
    }
}
