package us.koller.cameraroll.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import java.util.ArrayList;

import us.koller.cameraroll.R;
import us.koller.cameraroll.data.fileOperations.FileOperation;
import us.koller.cameraroll.data.Settings;
import us.koller.cameraroll.data.provider.MediaProvider;
import us.koller.cameraroll.util.Util;

//simple BaseActivity that handles LocalBroadcastReceivers, need for communication with FileOperationServices
public abstract class BaseActivity extends AppCompatActivity {

    private ArrayList<BroadcastReceiver> broadcastReceivers;

    private BroadcastReceiver removableStoragePermissionRequestBroadcastReceiver;

    //workIntent for FileOperation awaiting permission to write to removable storage
    private Intent workIntent;

    //snackbar to notify user Camera Roll is missing the storage permission
    private Snackbar snackbar;

    private boolean enterTransitionPostponed = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        broadcastReceivers = new ArrayList<>();

        BroadcastReceiver defaultBroadcastReceiver = getDefaultLocalBroadcastReceiver();
        if (defaultBroadcastReceiver != null) {
            registerLocalBroadcastReceiver(defaultBroadcastReceiver);
            broadcastReceivers.add(defaultBroadcastReceiver);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        //registering RemovableStorage...Receiver here so only the visible activity receives the broadcast
        removableStoragePermissionRequestBroadcastReceiver
                = getRemovableStoragePermissionRequestBroadcastReceiver();
        if (removableStoragePermissionRequestBroadcastReceiver != null) {
            IntentFilter filter = new IntentFilter();
            filter.addAction(FileOperation.NEED_REMOVABLE_STORAGE_PERMISSION);
            broadcastReceivers.add(removableStoragePermissionRequestBroadcastReceiver);
            LocalBroadcastManager.getInstance(this)
                    .registerReceiver(removableStoragePermissionRequestBroadcastReceiver, filter);
        }
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
                if (resultCode == RESULT_OK && workIntent != null) {
                    Uri treeUri = data.getData();
                    getContentResolver().takePersistableUriPermission(treeUri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION |
                                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                    Settings.getInstance(this).setRemovableStorageTreeUri(this, treeUri);

                    workIntent.putExtra(FileOperation.REMOVABLE_STORAGE_TREE_URI, treeUri.toString());
                    workIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                    startService(workIntent);
                    workIntent = null;
                }
                break;
            default:
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case MediaProvider.PERMISSION_REQUEST_CODE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //permission granted
                    onPermissionGranted();
                } else {
                    // permission denied
                    snackbar = Util.getPermissionDeniedSnackbar(findViewById(R.id.root_view));
                    snackbar.setAction(R.string.retry, new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            MediaProvider.checkPermission(BaseActivity.this);
                        }
                    });
                    Util.showSnackbar(snackbar);
                }
            }
            break;
            default:
                break;
        }
    }

    public void onPermissionGranted() {
        if (snackbar != null) {
            snackbar.dismiss();
        }
    }

    @Override
    protected void onDestroy() {
        Log.d("BaseActivity", "onDestroy() called " + this);
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
        if (broadcastReceiver != null) {
            broadcastReceivers.remove(broadcastReceiver);
            LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
        }
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
                                                startActivityForResult(requestIntent, MainActivity.REMOVABLE_STORAGE_PERMISSION_REQUEST_CODE);
                                            }
                                            dialog.dismiss();
                                        }
                                    }).setNegativeButton(getString(R.string.cancel), null)
                                    .create()
                                    .show();
                        }
                        break;
                    default:
                        break;
                }
            }
        };
    }

    public IntentFilter getBroadcastIntentFilter() {
        return new IntentFilter();
    }

    @Override
    public void postponeEnterTransition() {
        enterTransitionPostponed = true;
        super.postponeEnterTransition();
    }

    @Override
    public void startPostponedEnterTransition() {
        enterTransitionPostponed = false;
        super.startPostponedEnterTransition();
    }

    boolean enterTransitionPostponed() {
        return enterTransitionPostponed;
    }
}
