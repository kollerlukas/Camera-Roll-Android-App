package us.koller.cameraroll.data.models;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.PorterDuff;
import android.os.Parcel;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;

import us.koller.cameraroll.R;
import us.koller.cameraroll.data.Settings;
import us.koller.cameraroll.data.provider.Provider;
import us.koller.cameraroll.themes.Theme;
import us.koller.cameraroll.util.SortUtil;

public class VirtualAlbum extends Album {

    public static final String VIRTUAL_ALBUMS_DIR = "virtual_directory:";
    private static final String NAME = "NAME";
    private static final String REAL_DIRS = "REAL_DIRS";

    private String name;

    private ArrayList<String> directories;

    public VirtualAlbum(String name, String[] dirs) {
        super();
        this.name = name;
        directories = new ArrayList<>();
        directories.addAll(Arrays.asList(dirs));
    }

    public void create(Context context, ArrayList<Album> albums) {
        getAlbumItems().clear();
        for (int i = 0; i < albums.size(); i++) {
            Album album = albums.get(i);
            if (contains(album.getPath())) {
                getAlbumItems().addAll(album.getAlbumItems());
            }
        }
        Settings s = Settings.getInstance(context);
        int sortBy = s.sortAlbumBy();
        SortUtil.sort(getAlbumItems(), sortBy);

        pinned = Provider.isAlbumPinned(getPath(),
                Provider.getPinnedPaths());
    }

    public boolean contains(String path) {
        if (directories == null || path == null) {
            return false;
        }

        for (int i = 0; i < directories.size(); i++) {
            if (path.startsWith(directories.get(i))) {
                return true;
            }
        }
        return false;
    }

    public ArrayList<String> getDirectories() {
        return directories;
    }

    private void addDirectory(String path) {
        if (!directories.contains(path)) {
            directories.add(path);
        }
    }

    public void removeDirectory(String path) {
        directories.remove(path);
    }

    @Override
    public boolean isHidden() {
        return false;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Album setPath(String path) {
        super.setPath(path);
        return this;
    }

    @Override
    public String getPath() {
        return VIRTUAL_ALBUMS_DIR + getName();
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        super.writeToParcel(parcel, i);
        parcel.writeStringList(directories);
    }

    @SuppressWarnings("unused")
    public VirtualAlbum(Parcel parcel) {
        super(parcel);
        directories = parcel.createStringArrayList();
    }

    public VirtualAlbum(String jsonString) {
        try {
            JSONObject jsonObject = new JSONObject(jsonString);
            name = jsonObject.getString(NAME);
            JSONArray array = jsonObject.getJSONArray(REAL_DIRS);
            directories = new ArrayList<>();
            for (int i = 0; i < array.length(); i++) {
                directories.add(array.getString(i));
            }
        } catch (JSONException e) {
            e.printStackTrace();
            directories = new ArrayList<>();
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof VirtualAlbum) {
            VirtualAlbum virtualAlbum = (VirtualAlbum) obj;
            return virtualAlbum.getName().equals(getName());
        }
        return super.equals(obj);
    }

    @Override
    public String toString() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put(NAME, getName());
            JSONArray array = new JSONArray();
            for (int i = 0; i < directories.size(); i++) {
                array.put(directories.get(i));
            }
            jsonObject.put(REAL_DIRS, array);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject.toString();
    }

    public static class Util {

        public interface OnCreateVirtualAlbumCallback {
            void onVirtualAlbumCreated(VirtualAlbum virtualAlbum);
        }

        public static AlertDialog getCreateVirtualAlbumDialog(final Context context, final OnCreateVirtualAlbumCallback callback) {
            @SuppressLint("InflateParams")
            View dialogLayout = LayoutInflater.from(context)
                    .inflate(R.layout.input_dialog_layout, null, false);

            final EditText editText = dialogLayout.findViewById(R.id.edit_text);

            Theme theme = Settings.getInstance(context).getThemeInstance(context);

            final AlertDialog dialog = new AlertDialog.Builder(context, theme.getDialogThemeRes())
                    .setTitle(R.string.create_virtual_album)
                    .setView(dialogLayout)
                    .setPositiveButton(R.string.create, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int which) {
                            String name = editText.getText().toString();
                            ArrayList<VirtualAlbum> virtualAlbums = Provider.getVirtualAlbums(context);
                            for (int i = 0; i < virtualAlbums.size(); i++) {
                                if (virtualAlbums.get(i).getName().equals(name)) {
                                    Toast.makeText(context, R.string.virtual_album_different_name, Toast.LENGTH_SHORT).show();
                                    return;
                                }
                            }
                            VirtualAlbum virtualAlbum = new VirtualAlbum(name, new String[]{});
                            Provider.addVirtualAlbum(context, virtualAlbum);
                            Provider.saveVirtualAlbums(context);
                            callback.onVirtualAlbumCreated(virtualAlbum);
                            String message = context.getString(R.string.virtual_album_created, name);
                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton(R.string.cancel, null)
                    .create();
            //noinspection ConstantConditions
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
            return dialog;
        }

        public static AlertDialog getAddToVirtualAlbumDialog(final Context context, final String path) {
            ArrayList virtualAlbums = Provider.getVirtualAlbums(context);
            if (virtualAlbums.size() == 0) {
                return getCreateVirtualAlbumDialog(context, new OnCreateVirtualAlbumCallback() {
                    @Override
                    public void onVirtualAlbumCreated(VirtualAlbum virtualAlbum) {
                        virtualAlbum.addDirectory(path);
                        Provider.saveVirtualAlbums(context);
                    }
                });
            }

            @SuppressLint("InflateParams")
            View dialogLayout = LayoutInflater.from(context)
                    .inflate(R.layout.add_to_virtual_album_dialog, null, false);

            Theme theme = Settings.getInstance(context).getThemeInstance(context);

            final AlertDialog dialog = new AlertDialog.Builder(context, theme.getDialogThemeRes())
                    .setTitle(R.string.add_path_to_virtual_album)
                    .setView(dialogLayout)
                    .setNeutralButton(R.string.create_virtual_album, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int which) {
                            dialogInterface.dismiss();
                            AlertDialog dialog = getCreateVirtualAlbumDialog(context, new OnCreateVirtualAlbumCallback() {
                                @Override
                                public void onVirtualAlbumCreated(VirtualAlbum virtualAlbum) {
                                    virtualAlbum.addDirectory(path);
                                    Provider.saveVirtualAlbums(context);
                                }
                            });
                            dialog.show();
                        }
                    })
                    .setNegativeButton(R.string.cancel, null)
                    .create();
            //noinspection ConstantConditions
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);

            RecyclerView recyclerView = dialogLayout.findViewById(R.id.recyclerView);
            recyclerView.setLayoutManager(new LinearLayoutManager(context));
            recyclerView.setAdapter(new RecyclerViewAdapter(context,
                    new RecyclerViewAdapter.OnVirtualAlbumSelectedCallback() {
                        @Override
                        public void onVirtualAlbumSelected(VirtualAlbum virtualAlbum) {
                            virtualAlbum.addDirectory(path);
                            Provider.saveVirtualAlbums(context);
                            dialog.dismiss();
                            String message = context
                                    .getString(R.string.added_path_to_virtual_album, virtualAlbum.getName());
                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
                        }
                    }));

            final View scrollIndicatorTop = dialogLayout.findViewById(R.id.scroll_indicator_top);
            final View scrollIndicatorBottom = dialogLayout.findViewById(R.id.scroll_indicator_bottom);
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

            return dialog;
        }

        private static class RecyclerViewAdapter extends RecyclerView.Adapter {

            private ArrayList<VirtualAlbum> virtualAlbums;

            private OnVirtualAlbumSelectedCallback callback;

            public interface OnVirtualAlbumSelectedCallback {
                void onVirtualAlbumSelected(VirtualAlbum virtualAlbum);
            }

            private static class VirtualAlbumHolder extends RecyclerView.ViewHolder {

                private OnVirtualAlbumSelectedCallback callback;

                TextView textView;

                VirtualAlbumHolder(View itemView, OnVirtualAlbumSelectedCallback callback) {
                    super(itemView);
                    this.callback = callback;

                    Context context = itemView.getContext();
                    Theme theme = Settings.getInstance(context).getThemeInstance(context);
                    int accentColor = theme.getAccentColor(context);

                    textView = itemView.findViewById(R.id.text);
                    textView.setTextColor(accentColor);
                    ImageView folderIndicator = itemView.findViewById(R.id.folder_indicator);
                    folderIndicator.setColorFilter(accentColor, PorterDuff.Mode.SRC_IN);

                    itemView.findViewById(R.id.delete_button).setVisibility(View.GONE);
                }

                void bind(final VirtualAlbum album) {
                    textView.setText(album.getName());
                    itemView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            callback.onVirtualAlbumSelected(album);
                        }
                    });
                }
            }

            RecyclerViewAdapter(Context context, OnVirtualAlbumSelectedCallback callback) {
                virtualAlbums = Provider.getVirtualAlbums(context);
                this.callback = callback;
            }

            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                View v = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.virtual_album_cover, parent, false);
                return new VirtualAlbumHolder(v, callback);
            }

            @Override
            public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
                VirtualAlbum virtualAlbum = virtualAlbums.get(position);
                ((VirtualAlbumHolder) holder).bind(virtualAlbum);
            }

            @Override
            public int getItemCount() {
                return virtualAlbums.size();
            }
        }
    }
}
