package us.koller.cameraroll.adapter.fileExplorer;

import android.content.Intent;
import android.os.Handler;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import us.koller.cameraroll.R;
import us.koller.cameraroll.adapter.fileExplorer.ViewHolder.FileHolder;
import us.koller.cameraroll.data.Album;
import us.koller.cameraroll.data.AlbumItem;
import us.koller.cameraroll.data.File_POJO;
import us.koller.cameraroll.data.StorageRoot;
import us.koller.cameraroll.ui.FileExplorerActivity;
import us.koller.cameraroll.ui.ItemActivity;

public class RecyclerViewAdapter extends RecyclerView.Adapter {

    public static final int NORMAL_MODE = 0;
    public static final int SELECTOR_MODE = 1;
    public static final int PICK_TARGET_MODE = 2;


    public interface Callback {
        void onSelectorModeEnter();

        void onSelectorModeExit(File_POJO[] selected_items);

        void onItemSelected(int count);

        void onPickTargetModeEnter();

        void onPickTargetModeExit();

        void onDataChanged();
    }

    private File_POJO files;

    private int mode = NORMAL_MODE;
    private boolean[] selected_items;

    private Callback callback;

    private FileExplorerActivity.OnDirectoryChangeCallback directoryChangeCallback;

    public RecyclerViewAdapter(
            FileExplorerActivity.OnDirectoryChangeCallback directoryChangeCallback,
            Callback callback) {
        this.directoryChangeCallback = directoryChangeCallback;
        this.callback = callback;
    }

    public RecyclerViewAdapter setFiles(File_POJO files) {
        this.files = files;
        selected_items = new boolean[files.getChildren().size()];
        return this;
    }

    public File_POJO getFiles() {
        return files;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.file_cover, parent, false);
        return new FileHolder(v);
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, int position) {
        final File_POJO file = files.getChildren().get(position);

        ((FileHolder) holder).setFile(file);

        ((FileHolder) holder).setSelected(selected_items[position]);

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mode == SELECTOR_MODE) {
                    onItemSelect(file);
                } else if (file.isMedia) {

                    int index = file.getPath().lastIndexOf("/");
                    String path = file.getPath().substring(0, index);

                    //load Album
                    final Album album = new Album().setPath(path);
                    AlbumItem albumItem = AlbumItem.getInstance(holder.itemView.getContext(), file.getPath());
                    if (albumItem != null) {
                        album.getAlbumItems().add(albumItem);
                    }

                    if (album != null && albumItem != null) {
                        //create intent
                        Intent intent = new Intent(holder.itemView.getContext(), ItemActivity.class)
                                .putExtra(ItemActivity.ALBUM_ITEM, albumItem)
                                .putExtra(ItemActivity.ALBUM, album)
                                .putExtra(ItemActivity.ALBUM_PATH, album.getPath())
                                .putExtra(ItemActivity.VIEW_ONLY, true)
                                .putExtra(ItemActivity.ITEM_POSITION, album.getAlbumItems().indexOf(albumItem))
                                .putExtra(ItemActivity.FINISH_AFTER, false);

                        holder.itemView.getContext().startActivity(intent);
                    }
                } else {
                    //to keep the ripple animation
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            directoryChangeCallback.changeDir(file.getPath());
                        }
                    }, 300);
                }
            }
        });

        holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                onItemSelect(file);
                return true;
            }
        });

        //clicking on folder icons also selects this item
        holder.itemView.findViewById(R.id.folder_indicator).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onItemSelect(file);
            }
        });
    }

    private void onItemSelect(File_POJO file) {
        if (file instanceof StorageRoot) {
            return;
        }

        if (mode == NORMAL_MODE) {
            //no preselected Items
            enterSelectorMode(new File_POJO[0]);
            /*mode = SELECTOR_MODE;
            selected_items = new boolean[files.getChildren().size()];
            if (callback != null) {
                callback.onSelectorModeEnter();
            }*/
        }

        int position = files.getChildren().indexOf(file);
        selected_items[position] = !selected_items[position];
        notifyItemChanged(position);

        if (callback != null) {
            callback.onItemSelected(getSelectedCount());
        }

        checkSelectorMode();
    }

    public boolean isModeActive() {
        return mode == SELECTOR_MODE;
    }

    public int getMode() {
        return mode;
    }

    public int getSelectedCount() {
        int selected_items_count = 0;
        for (int i = 0; i < selected_items.length; i++) {
            selected_items_count += selected_items[i] ? 1 : 0;
        }
        return selected_items_count;
    }

    private void checkSelectorMode() {
        int selected_items_count = getSelectedCount();
        if (selected_items_count == 0) {
            cancelMode();
        }
    }

    public void cancelMode() {
        if (mode == SELECTOR_MODE) {
            mode = NORMAL_MODE;
            if (callback != null) {
                File_POJO[] files = getSelectedItems();
                callback.onSelectorModeExit(files);
            }
            selected_items = new boolean[files.getChildren().size()];
        } else if (mode == PICK_TARGET_MODE) {
            mode = NORMAL_MODE;
            if (callback != null) {
                callback.onPickTargetModeExit();
            }
        }
        notifyDataSetChanged();
    }

    public File_POJO[] getSelectedItems() {
        File_POJO[] files = new File_POJO[getSelectedCount()];
        int index = 0;
        for (int i = 0; i < selected_items.length; i++) {
            if (selected_items[i]) {
                files[index] = this.files.getChildren().get(i);
                index++;
            }
        }
        return files;
    }

    public void enterSelectorMode(File_POJO[] selectedItems) {
        mode = SELECTOR_MODE;
        selected_items = new boolean[files.getChildren().size()];
        //select items
        for (int i = 0; i < selectedItems.length; i++) {
            for (int k = 0; k < files.getChildren().size(); k++) {
                if (selectedItems[i].getPath()
                        .equals(files.getChildren().get(k).getPath())) {
                    onItemSelect(files.getChildren().get(k));
                }
            }
        }
        if (callback != null) {
            callback.onSelectorModeEnter();
        }
    }

    public void pickTarget() {
        mode = PICK_TARGET_MODE;
        if (callback != null) {
            callback.onPickTargetModeEnter();
        }
    }

    @Override
    public int getItemCount() {
        if (files != null) {
            return files.getChildren().size();
        }
        return 0;
    }
}