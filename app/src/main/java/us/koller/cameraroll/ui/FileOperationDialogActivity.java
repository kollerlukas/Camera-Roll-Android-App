package us.koller.cameraroll.ui;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;

import us.koller.cameraroll.R;
import us.koller.cameraroll.data.Album;
import us.koller.cameraroll.data.FileOperations.Copy;
import us.koller.cameraroll.data.FileOperations.FileOperation;
import us.koller.cameraroll.data.FileOperations.Move;
import us.koller.cameraroll.data.FileOperations.NewDirectory;
import us.koller.cameraroll.data.File_POJO;
import us.koller.cameraroll.data.Provider.MediaProvider;
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
                    MediaType.isMedia(this, filePaths[i]));
        }

        if (savedInstanceState != null
                && savedInstanceState.containsKey(CREATE_NEW_FOLDER)
                && savedInstanceState.getString(CREATE_NEW_FOLDER).equals("true")) {
            creatingNewFolder = true;
            createNewFolder(files);
            return;
        }

        showFolderSelectorDialog(files);
    }

    private interface NewFolderCallback {
        public void newFolderCreated(String path);

        public void failed();
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
    }

    public void showFolderSelectorDialog() {
        Intent intent = getIntent();
        if (intent == null) {
            return;
        }

        String[] filePaths = intent.getStringArrayExtra(FILES);
        final File_POJO[] files = new File_POJO[filePaths.length];
        for (int i = 0; i < filePaths.length; i++) {
            files[i] = new File_POJO(filePaths[i],
                    MediaType.isMedia(this, filePaths[i]));
        }

        showFolderSelectorDialog(files);
    }

    public void showFolderSelectorDialog(final File_POJO[] files) {
        View v = LayoutInflater.from(this)
                .inflate(R.layout.file_operation_dialog,
                        (ViewGroup) findViewById(R.id.root_view),
                        false);

        RecyclerView recyclerView = (RecyclerView) v.findViewById(R.id.recyclerView);

        recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        recyclerView.addItemDecoration(new GridMarginDecoration(
                (int) getResources().getDimension(R.dimen.album_grid_spacing_big)));

        final RecyclerViewAdapter recyclerViewAdapter = new RecyclerViewAdapter();
        recyclerView.setAdapter(recyclerViewAdapter);

        String title = (action.equals(ACTION_COPY) ? getString(R.string.copy) : getString(R.string.move)) +
                " " + files.length + (files.length > 1 ? getString(R.string.items) : getString(R.string.item)) + getString(R.string.to) + ":";

        dialog = new AlertDialog.Builder(this, getDialogThemeRes())
                .setTitle(title)
                .setView(v)
                .setPositiveButton(R.string.ok,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                String path = recyclerViewAdapter.getSelectedPath();
                                if (path != null) {
                                    executeAction(files, path);
                                }
                            }
                        })
                .setNeutralButton(getString(R.string.new_folder), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        creatingNewFolder = true;
                        createNewFolder(files);
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialogInterface) {
                        onDialogDismiss();
                    }
                })
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
        View dialogLayout = LayoutInflater.from(this).inflate(R.layout.new_folder_dialog,
                (ViewGroup) findViewById(R.id.root_view), false);

        final EditText editText = (EditText) dialogLayout.findViewById(R.id.edit_text);

        dialog = new AlertDialog.Builder(this, getDialogThemeRes())
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
                        new NewDirectory(new File_POJO[]{newFolder})
                                .execute(FileOperationDialogActivity.this,
                                        null,
                                        new FileOperation.Callback() {
                                            @Override
                                            public void done() {
                                                creatingNewFolder = false;
                                                callback.newFolderCreated(newFolder.getPath());
                                            }

                                            @Override
                                            public void failed(String path) {
                                                creatingNewFolder = false;
                                                callback.failed();
                                            }
                                        });
                    }
                })
                .setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        //showFolderSelectorDialog();
                    }
                })
                .setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialogInterface) {
                        creatingNewFolder = false;
                        onDialogDismiss();
                    }
                })
                .create();
        dialog.show();
    }

    public void executeAction(File_POJO[] files, String target) {
        FileOperation fileOperation;
        if (action.equals(ACTION_COPY)) {
            fileOperation = new Copy(files);
        } else {
            fileOperation = new Move(files);
        }
        fileOperation.execute(this,
                new File_POJO(target, false),
                new FileOperation.Callback() {
                    @Override
                    public void done() {

                    }

                    @Override
                    public void failed(String path) {

                    }
                });
    }

    @Override
    public int getThemeRes(int style) {
        if (style == DARK) {
            return R.style.Theme_CameraRoll_Translucent_FileOperationDialog;
        } else {
            return R.style.Theme_CameraRoll_Translucent_Light_FileOperationDialog;
        }
    }

    @Override
    public void onThemeApplied(int theme) {

    }

    private static class RecyclerViewAdapter extends RecyclerView.Adapter {

        static class ViewHolder extends RecyclerView.ViewHolder {

            public ViewHolder(View itemView) {
                super(itemView);
            }
        }

        private ArrayList<Album> albums;

        private int selected_position = -1;

        RecyclerViewAdapter() {
            albums = MediaProvider.getAlbums();

            if (albums.size() == 0) {
                albums.add(MediaProvider.getErrorAlbum());
            }
        }

        String getSelectedPath() {
            if (selected_position == -1) {
                return null;
            }

            return albums.get(selected_position).getPath();
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.file_op_view_holder, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, final int position) {
            final Album album = albums.get(position);
            ((TextView) holder.itemView.findViewById(R.id.album_title))
                    .setText(album.getName());

            if (album.getAlbumItems().size() > 0) {
                Glide.with(holder.itemView.getContext())
                        .load(album.getAlbumItems().get(0).getPath())
                        .error(R.drawable.error_placeholder_tinted)
                        .into((ImageView) holder.itemView.findViewById(R.id.image));
            }

            final Drawable selectorOverlay = Util
                    .getAlbumItemSelectorOverlay(holder.itemView.getContext());

            final View view = holder.itemView.findViewById(R.id.image);
            if (selected_position == position) {
                view.post(new Runnable() {
                    @Override
                    public void run() {
                        view.getOverlay().clear();
                        selectorOverlay.setBounds(0, 0,
                                view.getWidth(),
                                view.getHeight());
                        view.getOverlay().add(selectorOverlay);
                    }
                });
            } else {
                view.post(new Runnable() {
                    @Override
                    public void run() {
                        view.getOverlay().clear();
                    }
                });
            }

            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    //un-select old item
                    notifyItemChanged(selected_position);
                    selected_position = position;
                    //select new item
                    notifyItemChanged(position);
                }
            });
        }

        @Override
        public int getItemCount() {
            return albums.size();
        }
    }
}
