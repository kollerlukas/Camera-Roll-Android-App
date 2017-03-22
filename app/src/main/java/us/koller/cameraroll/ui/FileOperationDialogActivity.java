package us.koller.cameraroll.ui;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
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
import us.koller.cameraroll.util.MediaType;
import us.koller.cameraroll.util.Util;

public class FileOperationDialogActivity extends AppCompatActivity {

    public static String ACTION_COPY = "ACTION_COPY";
    public static String ACTION_MOVE = "ACTION_MOVE";

    public static String FILES = "FILES";
    private static String CREATE_NEW_FOLDER = "CREATE_NEW_FOLDER";

    private String action;

    private boolean creatingNewFolder = false;

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

        View v = LayoutInflater.from(this)
                .inflate(R.layout.file_operation_dialog,
                        (ViewGroup) findViewById(R.id.root_view),
                        false);

        RecyclerView recyclerView = (RecyclerView) v.findViewById(R.id.recyclerView);

        recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        final RecyclerViewAdapter recyclerViewAdapter = new RecyclerViewAdapter();
        recyclerView.setAdapter(recyclerViewAdapter);

        String title = (action.equals(ACTION_COPY) ? getString(R.string.copy) : getString(R.string.move)) +
                " " + filePaths.length + (filePaths.length > 1 ? getString(R.string.items) : getString(R.string.item)) + getString(R.string.to) + ":";

        AlertDialog dialog =
                new AlertDialog.Builder(this)
                        .setTitle(title)
                        .setView(v)
                        .setPositiveButton(R.string.ok,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        String path = recyclerViewAdapter.getSelectedPath();
                                        if (path != null) {
                                            if (path.equals(CREATE_NEW_FOLDER)) {
                                                creatingNewFolder = true;
                                                createNewFolder(new NewFolderCallback() {
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
                                            } else {
                                                executeAction(files, path);
                                            }
                                        }
                                    }
                                })
                        .setNegativeButton(R.string.cancel, null)
                        .setOnDismissListener(new DialogInterface.OnDismissListener() {
                            @Override
                            public void onDismiss(DialogInterface dialogInterface) {
                                if (!creatingNewFolder) {
                                    setResult(RESULT_CANCELED, null);
                                    finish();
                                }
                            }
                        })
                        .create();

        dialog.show();
    }

    private interface NewFolderCallback {
        public void newFolderCreated(String path);

        public void failed();
    }

    public void createNewFolder(final NewFolderCallback callback) {
        View dialogLayout = LayoutInflater.from(this).inflate(R.layout.new_folder_dialog,
                (ViewGroup) findViewById(R.id.root_view), false);

        final EditText editText = (EditText) dialogLayout.findViewById(R.id.edit_text);

        new AlertDialog.Builder(this)
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
                                                callback.newFolderCreated(newFolder.getPath());
                                            }

                                            @Override
                                            public void failed(String path) {
                                                callback.failed();
                                            }
                                        });
                    }
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialogInterface) {
                        setResult(RESULT_CANCELED, null);
                        finish();
                    }
                })
                .create().show();
    }

    public void executeAction(File_POJO[] files, String target) {
        FileOperation fileOperation;
        if (action.equals(ACTION_COPY)) {
            fileOperation = new Copy(files);
        } else {
            fileOperation = new Move(files);
        }
        fileOperation.execute(FileOperationDialogActivity.this,
                new File_POJO(target, false), null);
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

            if (albums.size() > selected_position) {
                return albums.get(selected_position).getPath();
            } else {
                return CREATE_NEW_FOLDER;
            }
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.file_op_view_holder, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, final int position) {
            if (albums.size() > position) {
                Album album = albums.get(position);
                ((TextView) holder.itemView.findViewById(R.id.album_title))
                        .setText(album.getName());

                if (album.getAlbumItems().size() > 0) {
                    Glide.with(holder.itemView.getContext())
                            .load(album.getAlbumItems().get(0).getPath())
                            .asBitmap()
                            .error(R.drawable.error_placeholder_tinted)
                            .into((ImageView) holder.itemView.findViewById(R.id.image));
                }
            } else {
                ((TextView) holder.itemView.findViewById(R.id.album_title))
                        .setText(holder.itemView.getContext().getString(R.string.new_folder));

                Glide.with(holder.itemView.getContext())
                        .load(R.drawable.new_folder_placeholder)
                        .into((ImageView) holder.itemView.findViewById(R.id.image));
            }

            final Drawable selectorOverlay = Util
                    .getAlbumItemSelectorOverlay(holder.itemView.getContext());

            final View view = holder.itemView;
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
            return albums.size() + 1;
        }
    }
}
