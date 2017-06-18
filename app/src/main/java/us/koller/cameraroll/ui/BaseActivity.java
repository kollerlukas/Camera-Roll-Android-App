package us.koller.cameraroll.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import java.util.ArrayList;

import us.koller.cameraroll.R;
import us.koller.cameraroll.data.FileOperations.FileOperation;

//simple BaseActivity that handles LocalBroadcastReceivers, need for communication with FileOperationServices
public abstract class BaseActivity extends AppCompatActivity {

    private ArrayList<BroadcastReceiver> broadcastReceivers;

    private BroadcastReceiver removableStoragePermissionRequestBroadcastReceiver;

    //workIntent for FileOperation awaiting permission to write to removable storage
    private Intent workIntent;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        broadcastReceivers = new ArrayList<>();

        BroadcastReceiver defaultBroadcastReceiver = getDefaultLocalBroadcastReceiver();
        if (defaultBroadcastReceiver != null) {
            registerLocalBroadcastReceiver(defaultBroadcastReceiver);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        //registering RemovableStorage...Receiver here so only the visible activity receives the broadcast
        removableStoragePermissionRequestBroadcastReceiver
                = getRemovableStoragePermissionRequestBroadcastReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(FileOperation.NEED_REMOVABLE_STORAGE_PERMISSION);
        broadcastReceivers.add(removableStoragePermissionRequestBroadcastReceiver);
        LocalBroadcastManager.getInstance(this)
                .registerReceiver(removableStoragePermissionRequestBroadcastReceiver, filter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterLocalBroadcastReceiver(removableStoragePermissionRequestBroadcastReceiver);
        removableStoragePermissionRequestBroadcastReceiver = null;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case MainActivity.REMOVABLE_STORAGE_PERMISSION_REQUEST_CODE:
                Log.d("BaseActivity", "onActivityResult: REMOVABLE_STORAGE_PERMISSION_REQUEST_CODE");
                if (resultCode == RESULT_OK && workIntent != null) {
                    Uri treeUri = data.getData();
                    Log.d("BaseActivity", "treeUri: " + treeUri);
                    getContentResolver().takePersistableUriPermission(treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION |
                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

                    workIntent.putExtra(FileOperation.REMOVABLE_STORAGE_TREE_URI, treeUri.toString());
                    workIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                    startService(workIntent);
                    workIntent = null;
                }
                break;
        }
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

    public BroadcastReceiver getDefaultLocalBroadcastReceiver() {
        return null;
    }

    public BroadcastReceiver getRemovableStoragePermissionRequestBroadcastReceiver() {
        return new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d("BaseActivity", "onReceive: " + intent.getAction() + ", " + BaseActivity.this);
                switch (intent.getAction()) {
                    case FileOperation.NEED_REMOVABLE_STORAGE_PERMISSION:
                        final Intent workIntent = intent.getParcelableExtra(FileOperation.WORK_INTENT);
                        if (workIntent != null) {
                            new AlertDialog.Builder(BaseActivity.this)
                                    .setTitle(R.string.grant_removable_storage_permission)
                                    .setMessage(R.string.grant_removable_storage_permission_message)
                                    .setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            BaseActivity.this.workIntent = workIntent;
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                                Intent requestIntent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                                                startActivityForResult(requestIntent,
                                                        MainActivity.REMOVABLE_STORAGE_PERMISSION_REQUEST_CODE);
                                            }
                                            dialog.dismiss();
                                        }
                                    }).setNegativeButton(getString(R.string.cancel), null)
                                    .create()
                                    .show();
                        }
                        break;
                }
            }
        };
    }

    public IntentFilter getBroadcastIntentFilter() {
        return new IntentFilter();
    }
}
