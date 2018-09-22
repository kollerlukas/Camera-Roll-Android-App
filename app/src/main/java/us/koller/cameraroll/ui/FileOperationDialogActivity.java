package us.koller.cameraroll.ui;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import java.io.File;
import java.util.ArrayList;
import java.util.Objects;

import us.koller.cameraroll.R;
import us.koller.cameraroll.data.Settings;
import us.koller.cameraroll.data.fileOperations.FileOperation;
import us.koller.cameraroll.data.models.Album;
import us.koller.cameraroll.data.models.AlbumItem;
import us.koller.cameraroll.data.models.File_POJO;
import us.koller.cameraroll.data.provider.MediaProvider;
import us.koller.cameraroll.ui.widget.GridMarginDecoration;
import us.koller.cameraroll.util.MediaType;
import us.koller.cameraroll.util.Util;

public class FileOperationDialogActivity extends ThemeableActivity {

    public static String ACTION_COPY = "ACTION_COPY";
    public static String ACTION_MOVE = "ACTION_MOVE";

    public static String FILES = "FILES";
    private static String CREATE_NEW_FOLDER = "CREATE_NEW_FOLDER";

    private String action;

    private boolean creatingNewFolder = false;

    private AlertDialog dialog;

    // need to start FileOperation, when this activity is destroyed
    // otherwise running into issue with the removable storage permission broadcast not being received
    private OnDestroyListener onDestroyListener;

    private interface OnDestroyListener {
        void onDestroy();
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_operation_dialog);

        Intent intent = getIntent();

        if (intent == null) {
            this.finish();
            return;
        }

        action = intent.getAction();

        String[] filePaths = intent.getStringArrayExtra(FILES);
        final File_POJO[] files = new File_POJO[filePaths.length];
        for (int i = 0; i < filePaths.length; i++) {
            files[i] = new File_POJO(filePaths[i],
                    MediaType.isMedia(filePaths[i]));
        }

        if (savedInstanceState != null
                && savedInstanceState.containsKey(CREATE_NEW_FOLDER)
                && Objects.equals(savedInstanceState.getString(CREATE_NEW_FOLDER), "true")) {
            creatingNewFolder = true;
            createNewFolder(files);
            return;
        }

        showFolderSelectorDialog(files);
    }

    private interface NewFolderCallback {
        void newFolderCreated(String path);

        void failed();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (creatingNewFolder) {
            outState.putString(CREATE_NEW_FOLDER, "true");
        }
    }

    public void onDialogDismiss() {
        if (!(creatingNewFolder || isChangingConfigurations())) {
            setResult(RESULT_CANCELED, null);
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (dialog != null) {
            dialog.dismiss();
        }

        if (onDestroyListener != null) {
            onDestroyListener.onDestroy();
        }
    }

    public void showFolderSelectorDialog(final File_POJO[] files) {
        View v = LayoutInflater.from(this)
                .inflate(R.layout.file_operation_dialog,
                        findViewById(R.id.root_view),
                        false);

        RecyclerView recyclerView = v.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        recyclerView.addItemDecoration(new GridMarginDecoration((int) getResources().getDimension(R.dimen.album_grid_spacing)));

        final RecyclerViewAdapter recyclerViewAdapter = new RecyclerViewAdapter();
        recyclerView.setAdapter(recyclerViewAdapter);

        final View scrollIndicatorTop = v.findViewById(R.id.scroll_indicator_top);
        final View scrollIndicatorBottom = v.findViewById(R.id.scroll_indicator_bottom);

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                scrollIndicatorTop.setVisibility(
                        recyclerView.canScrollVertically(-1) ?
                                View.VISIBLE : View.INVISIBLE);

                scrollIndicatorBottom.setVisibility(
                        recyclerView.canScrollVertically(1) ?
                                View.VISIBLE : View.INVISIBLE);
            }
        });

        int stringRes;
        boolean oneItem = files.length == 1;
        if (action.equals(ACTION_COPY)) {
            if (oneItem) {
                stringRes = R.string.copy_item_to;
            } else {
                stringRes = R.string.copy_items_to;
            }
        } else {
            if (oneItem) {
                stringRes = R.string.move_item_to;
            } else {
                stringRes = R.string.move_items_to;
            }
        }
        String title = getString(stringRes, files.length);

        dialog = new AlertDialog.Builder(this, theme.getDialogThemeRes())
                .setTitle(title)
                .setView(v)
                .setPositiveButton(R.string.ok, (dialogInterface, i) ->
                        onDestroyListener = () -> {
                            String path = recyclerViewAdapter.getSelectedPath();
                            if (path != null) {
                                executeAction(files, path);
                            }
                        })
                .setNeutralButton(getString(R.string.new_folder), (dialogInterface, i) -> {
                    creatingNewFolder = true;
                    createNewFolder(files);
                })
                .setNegativeButton(R.string.cancel, null)
                .setOnDismissListener(dialogInterface -> onDialogDismiss())
                .create();

        dialog.show();
    }

    public void createNewFolder(final File_POJO[] files) {
        createNewFolderDialog(new NewFolderCallback() {
            @Override
            public void newFolderCreated(String path) {
                executeAction(files, path);
            }

            @Override
            public void failed() {
                setResult(RESULT_CANCELED, null);
                finish();
            }
        });
    }

    public void createNewFolderDialog(final NewFolderCallback callback) {
        View dialogLayout = LayoutInflater.from(this).inflate(R.layout.input_dialog_layout,
                findViewById(R.id.root_view), false);

        final EditText editText = dialogLayout.findViewById(R.id.edit_text);

        dialog = new AlertDialog.Builder(this, theme.getDialogThemeRes())
                .setTitle(R.string.new_folder)
                .setView(dialogLayout)
                .setPositiveButton(R.string.create, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        String filename = editText.getText().toString();
                        String picturesDir = Environment
                                .getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getPath();

                        final File_POJO newFolder
                                = new File_POJO(picturesDir + "/" + filename, false);

                        registerLocalBroadcastReceiver(new BroadcastReceiver() {
                            @Override
                            public void onReceive(Context context, Intent intent) {
                                unregisterLocalBroadcastReceiver(this);
                                switch (intent.getAction()) {
                                    case FileOperation.RESULT_DONE:
                                        creatingNewFolder = false;
                                        callback.newFolderCreated(newFolder.getPath());
                                        break;
                                    case FileOperation.FAILED:
                                        creatingNewFolder = false;
                                        callback.failed();
                                        break;
                                    default:
                                        break;
                                }
                            }
                        });

                        Intent intent = FileOperation.getDefaultIntent(
                                FileOperationDialogActivity.this,
                                FileOperation.NEW_DIR,
                                new File_POJO[]{newFolder});
                        startService(intent);
                    }
                })
                .setNegativeButton(getString(R.string.cancel), (dialogInterface, i) -> {
                    //showFolderSelectorDialog();
                })
                .setOnDismissListener(dialogInterface -> {
                    creatingNewFolder = false;
                    onDialogDismiss();
                })
                .create();
        //noinspection ConstantConditions
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        dialog.show();
    }

    public void executeAction(File_POJO[] files, String target) {
        int action = this.action.equals(ACTION_COPY) ? FileOperation.COPY : FileOperation.MOVE;
        final Intent workIntent = FileOperation.getDefaultIntent(this, action, files);
        workIntent.putExtra(FileOperation.TARGET, new File_POJO(target, false));
        startService(workIntent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case MainActivity.REMOVABLE_STORAGE_PERMISSION_REQUEST_CODE:
                onDialogDismiss();
                break;
            default:
                break;
        }
    }

    @Override
    public int getDarkThemeRes() {
        return R.style.CameraRoll_Theme_Translucent_FileOperationDialog;
    }

    @Override
    public int getLightThemeRes() {
        return R.style.CameraRoll_Theme_Light_Translucent_FileOperationDialog;
    }

    @Override
    public IntentFilter getBroadcastIntentFilter() {
        return FileOperation.Util.getIntentFilter(super.getBroadcastIntentFilter());
    }

    private static class RecyclerViewAdapter extends RecyclerView.Adapter {

        private ArrayList<Album> albums;
        private int selected_position = -1;

        @Override
        public void onBindViewHolder(@NonNull final RecyclerView.ViewHolder holder,
                                     @SuppressLint("RecyclerView") final int position) {
            final Album album = albums.get(position);
            ((TextView) holder.itemView.findViewById(R.id.name))
                    .setText(album.getName());

            int itemCount = album.getAlbumItems().size();
            boolean oneItem = itemCount == 1;
            String count = holder.itemView.getContext().getString(oneItem ?
                    R.string.item_count : R.string.items_count, itemCount);
            ((TextView) holder.itemView.findViewById(R.id.count))
                    .setText(count);

            final boolean selected = position == selected_position;
            ((ViewHolder) holder).setSelected(selected);

            if (album.getAlbumItems().size() > 0) {

                AlbumItem albumItem = album.getAlbumItems().get(0);

                RequestOptions options = new RequestOptions()
                        .error(R.drawable.error_placeholder)
                        .signature(albumItem.getGlideSignature());

                Glide.with(holder.itemView.getContext())
                        .load(albumItem.getPath())
                        .apply(options)
                        .into((ImageView) holder.itemView.findViewById(R.id.image));

                boolean onRemovableStorage = false;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                    try {
                        onRemovableStorage = Environment.isExternalStorageRemovable(new File(album.getPath()));
                    } catch (IllegalArgumentException e) {
                        e.printStackTrace();
                    }
                }
                ImageView removableStorageIndicator = holder.itemView.findViewById(R.id.removable_storage_indicator);
                removableStorageIndicator.setVisibility(onRemovableStorage ? View.VISIBLE : View.GONE);
            }

            holder.itemView.setOnClickListener(view -> {
                int oldSelectedPosition = selected_position;
                if (selected_position != position) {
                    //un-select old item
                    notifyItemChanged(oldSelectedPosition);
                    selected_position = position;
                }
                //select new item
                notifyItemChanged(selected_position);
            });
        }

        RecyclerViewAdapter() {
            albums = MediaProvider.getAlbums();
            if (albums != null && albums.size() == 0) {
                albums.add(MediaProvider.getErrorAlbum());
            }
        }

        String getSelectedPath() {
            if (selected_position == -1) {
                return null;
            }
            return albums.get(selected_position).getPath();
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.file_op_view_holder, parent, false);
            return new ViewHolder(v);
        }

        static class ViewHolder extends RecyclerView.ViewHolder {

            public ViewHolder(View itemView) {
                super(itemView);
            }

            private void setSelected(boolean selected) {
                //final View imageView = itemView.findViewById(R.id.image);
                final View card = itemView.findViewById(R.id.card);

                if (selected) {
                    final Drawable selectorOverlay = Util
                            .getAlbumItemSelectorOverlay(card.getContext());
                    Context context = card.getContext();
                    int tintColor = Settings.getInstance(context)
                            .getThemeInstance(context)
                            .getAccentColorLight(context);
                    final Drawable colorDrawable1 = new ColorDrawable(tintColor),
                            colorDrawable2 = new ColorDrawable(tintColor);
                    colorDrawable1.setAlpha(138);
                    colorDrawable2.setAlpha(138);
                    card.post(() -> {
                        card.getOverlay().clear();
                        if (selectorOverlay != null) {
                            int width = card.getWidth(), height = card.getHeight();
                            int start = (width - height) / 2;
                            //noinspection SuspiciousNameCombination
                            selectorOverlay.setBounds(start, 0, start + height, height);
                            colorDrawable1.setBounds(0, 0, start, height);
                            colorDrawable2.setBounds(start + height, 0, width, height);
                            card.getOverlay().add(selectorOverlay);
                            card.getOverlay().add(colorDrawable1);
                            card.getOverlay().add(colorDrawable2);
                        }
                    });
                } else {
                    card.post(card.getOverlay()::clear);
                }
            }
        }

        @Override
        public int getItemCount() {
            return albums != null ? albums.size() : 0;
        }
    }
}
