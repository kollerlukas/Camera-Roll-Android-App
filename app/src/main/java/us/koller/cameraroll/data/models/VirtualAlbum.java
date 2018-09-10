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

    @SuppressWarnings("unused")
    public VirtualAlbum(Parcel p) {
        super(p);
        directories = p.createStringArrayList();
    }

    public VirtualAlbum(String jsonString) {
        try {
            JSONObject j = new JSONObject(jsonString);
            name = j.getString(NAME);
            JSONArray array = j.getJSONArray(REAL_DIRS);
            directories = new ArrayList<>();
            for (int i = 0; i < array.length(); i++) {
                directories.add(array.getString(i));
            }
        } catch (JSONException e) {
            e.printStackTrace();
            directories = new ArrayList<>();
        }
    }

    public void create(Context c, ArrayList<Album> albums) {
        getAlbumItems().clear();
        for (int i = 0; i < albums.size(); i++) {
            Album a = albums.get(i);
            if (contains(a.getPath())) {
                getAlbumItems().addAll(a.getAlbumItems());
            }
        }
        Settings s = Settings.getInstance(c);
        int sortBy = s.sortAlbumBy();
        SortUtil.sort(getAlbumItems(), sortBy);

        pinned = Provider.isAlbumPinned(getPath(),
                Provider.getPinnedPaths());
    }

    private void addDirectory(String path) {
        if (!directories.contains(path)) {
            directories.add(path);
        }
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

    public void removeDirectory(String path) {
        directories.remove(path);
    }

    @Override
    public Album setPath(String path) {
        super.setPath(path);
        return this;
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
    public String getPath() {
        return VIRTUAL_ALBUMS_DIR + getName();
    }

    @Override
    public void writeToParcel(Parcel p, int i) {
        super.writeToParcel(p, i);
        p.writeStringList(directories);
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof VirtualAlbum) {
            VirtualAlbum vA = (VirtualAlbum) o;
            return vA.getName().equals(getName());
        }
        return super.equals(o);
    }

    @Override
    public String toString() {
        JSONObject j = new JSONObject();
        try {
            j.put(NAME, getName());
            JSONArray array = new JSONArray();
            for (int i = 0; i < directories.size(); i++) {
                array.put(directories.get(i));
            }
            j.put(REAL_DIRS, array);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return j.toString();
    }

    public static class Util {

        public static AlertDialog getCreateVirtualAlbumDialog(final Context c, final OnCreateVirtualAlbumCallback ca) {
            @SuppressLint("InflateParams")
            View dialogLayout = LayoutInflater.from(c)
                    .inflate(R.layout.input_dialog_layout, null, false);

            final EditText editText = dialogLayout.findViewById(R.id.edit_text);

            Theme theme = Settings.getInstance(c).getThemeInstance(c);

            final AlertDialog dialog = new AlertDialog.Builder(c, theme.getDialogThemeRes())
                    .setTitle(R.string.create_virtual_album)
                    .setView(dialogLayout)
                    .setPositiveButton(R.string.create, (DialogInterface dialogInterface, int which) -> {
                        String name = editText.getText().toString();
                        ArrayList<VirtualAlbum> vAs = Provider.getVirtualAlbums(c);
                        for (int i = 0; i < vAs.size(); i++) {
                            if (vAs.get(i).getName().equals(name)) {
                                Toast.makeText(c, R.string.virtual_album_different_name, Toast.LENGTH_SHORT).show();
                                return;
                            }
                        }
                        VirtualAlbum vA = new VirtualAlbum(name, new String[]{});
                        Provider.addVirtualAlbum(c, vA);
                        Provider.saveVirtualAlbums(c);
                        ca.onVirtualAlbumCreated(vA);
                        String message = c.getString(R.string.virtual_album_created, name);
                        Toast.makeText(c, message, Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton(R.string.cancel, null)
                    .create();
            //noinspection ConstantConditions
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
            return dialog;
        }

        public static AlertDialog getAddToVirtualAlbumDialog(final Context c, final String path) {
            ArrayList vAs = Provider.getVirtualAlbums(c);
            if (vAs.size() == 0) {
                return getCreateVirtualAlbumDialog(c, (VirtualAlbum vA) -> {
                    vA.addDirectory(path);
                    Provider.saveVirtualAlbums(c);
                });
            }

            @SuppressLint("InflateParams")
            View dialogLayout = LayoutInflater.from(c)
                    .inflate(R.layout.add_to_virtual_album_dialog, null, false);

            Theme theme = Settings.getInstance(c).getThemeInstance(c);

            final AlertDialog dialog = new AlertDialog.Builder(c, theme.getDialogThemeRes())
                    .setTitle(R.string.add_path_to_virtual_album)
                    .setView(dialogLayout)
                    .setNeutralButton(R.string.create_virtual_album, (DialogInterface dialogInterface, int which) -> {
                        dialogInterface.dismiss();
                        AlertDialog d = getCreateVirtualAlbumDialog(c, (VirtualAlbum vA) -> {
                            vA.addDirectory(path);
                            Provider.saveVirtualAlbums(c);
                        });
                        d.show();
                    })
                    .setNegativeButton(R.string.cancel, null)
                    .create();
            //noinspection ConstantConditions
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);

            RecyclerView rV = dialogLayout.findViewById(R.id.recyclerView);
            rV.setLayoutManager(new LinearLayoutManager(c));
            rV.setAdapter(new RecyclerViewAdapter(c, (VirtualAlbum vA) -> {
                vA.addDirectory(path);
                Provider.saveVirtualAlbums(c);
                dialog.dismiss();
                String message = c
                        .getString(R.string.added_path_to_virtual_album, vA.getName());
                Toast.makeText(c, message, Toast.LENGTH_SHORT).show();
            }));

            final View scrollIndicatorTop = dialogLayout.findViewById(R.id.scroll_indicator_top);
            final View scrollIndicatorBottom = dialogLayout.findViewById(R.id.scroll_indicator_bottom);
            rV.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(RecyclerView rv, int dx, int dy) {
                    super.onScrolled(rv, dx, dy);
                    scrollIndicatorTop.setVisibility(
                            rv.canScrollVertically(-1) ?
                                    View.VISIBLE : View.INVISIBLE);
                    scrollIndicatorBottom.setVisibility(
                            rv.canScrollVertically(1) ?
                                    View.VISIBLE : View.INVISIBLE);
                }
            });
            return dialog;
        }

        public interface OnCreateVirtualAlbumCallback {
            void onVirtualAlbumCreated(VirtualAlbum vA);
        }

        private static class RecyclerViewAdapter extends RecyclerView.Adapter {
            private ArrayList<VirtualAlbum> virtualAlbums;
            private OnVirtualAlbumSelectedCallback callback;

            RecyclerViewAdapter(Context c, OnVirtualAlbumSelectedCallback ca) {
                virtualAlbums = Provider.getVirtualAlbums(c);
                this.callback = ca;
            }

            @Override
            public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
                VirtualAlbum vA = virtualAlbums.get(position);
                ((VirtualAlbumHolder) holder).bind(vA);
            }

            @Override
            public int getItemCount() {
                return virtualAlbums.size();
            }

            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                View v = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.virtual_album_cover, parent, false);
                return new VirtualAlbumHolder(v, callback);
            }

            public interface OnVirtualAlbumSelectedCallback {
                void onVirtualAlbumSelected(VirtualAlbum vA);
            }

            private static class VirtualAlbumHolder extends RecyclerView.ViewHolder {
                private OnVirtualAlbumSelectedCallback callback;
                TextView textView;

                VirtualAlbumHolder(View itemView, OnVirtualAlbumSelectedCallback ca) {
                    super(itemView);
                    this.callback = ca;

                    Context c = itemView.getContext();
                    Theme theme = Settings.getInstance(c).getThemeInstance(c);
                    int accentColor = theme.getAccentColor(c);

                    textView = itemView.findViewById(R.id.text);
                    textView.setTextColor(accentColor);
                    ImageView folderIndicator = itemView.findViewById(R.id.folder_indicator);
                    folderIndicator.setColorFilter(accentColor, PorterDuff.Mode.SRC_IN);

                    itemView.findViewById(R.id.delete_button).setVisibility(View.GONE);
                }

                void bind(final VirtualAlbum album) {
                    textView.setText(album.getName());
                    itemView.setOnClickListener((View v) -> callback.onVirtualAlbumSelected(album));
                }
            }
        }
    }
}
