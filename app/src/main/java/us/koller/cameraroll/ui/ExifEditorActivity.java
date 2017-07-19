package us.koller.cameraroll.ui;

import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.media.ExifInterface;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowInsets;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;

import us.koller.cameraroll.R;
import us.koller.cameraroll.themes.Theme;
import us.koller.cameraroll.data.AlbumItem;
import us.koller.cameraroll.util.ExifUtil;
import us.koller.cameraroll.util.MediaType;
import us.koller.cameraroll.util.Util;

//simple Activity to edit the Exif-Data of images
public class ExifEditorActivity extends ThemeableActivity {

    public static final String ALBUM_ITEM = "ALBUM_ITEM";
    public static final String EDITED_ITEMS = "EDITED_ITEMS";

    private Menu menu;

    private ExifInterface exifInterface;

    private ArrayList<EditedItem> editedItems;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_exif_editor);

        AlbumItem albumItem = getIntent().getParcelableExtra(ALBUM_ITEM);

        if (savedInstanceState != null && savedInstanceState.containsKey(EDITED_ITEMS)) {
            editedItems = savedInstanceState.getParcelableArrayList(EDITED_ITEMS);
        } else {
            editedItems = new ArrayList<>();
        }

        if (albumItem == null) {
            this.finish();
            return;
        }

        String mimeType = MediaType.getMimeType(this, albumItem.getUri(this));
        if (!MediaType.doesSupportExif_MimeType(mimeType)) {
            this.finish();
            return;
        }

        exifInterface = null;
        try {
            //cannot save changes with the inputStream as input
            /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Uri uri = albumItem.getUri(this);
                exifInterface = new ExifInterface(getContentResolver().openInputStream(uri));
            } else {
                exifInterface = new ExifInterface(albumItem.getPath());
            }*/

            exifInterface = new ExifInterface(albumItem.getPath());
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (exifInterface == null) {
            this.finish();
            return;
        }

        final Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        final RecyclerView recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(new RecyclerViewAdapter(exifInterface,
                new RecyclerViewAdapter.OnEditCallback() {
                    @Override
                    public void onItemEdited(String tag, String newValue) {
                        String oldValue = exifInterface.getAttribute(tag);
                        if (oldValue == null) {
                            oldValue = "";
                        }
                        if (oldValue.equals(newValue)) {
                            return;
                        }

                        //check if item was already edited
                        boolean alreadyInEditedItems = false;
                        for (int i = 0; i < editedItems.size(); i++) {
                            if (editedItems.get(i).tag.equals(tag)) {
                                alreadyInEditedItems = true;
                            }
                        }

                        if (!alreadyInEditedItems) {
                            editedItems.add(new EditedItem(tag, newValue));
                        } else {
                            for (int i = 0; i < editedItems.size(); i++) {
                                if (editedItems.get(i).tag.equals(tag)) {
                                    EditedItem item = editedItems.get(i);
                                    item.setNewValue(newValue);
                                }
                            }
                        }

                        //make save button visible
                        if (editedItems.size() > 0) {
                            MenuItem save = menu.findItem(R.id.save);
                            save.setVisible(true);
                        }
                    }

                    @Override
                    public EditedItem getEditedItem(String constant) {
                        for (int i = 0; i < editedItems.size(); i++) {
                            if (editedItems.get(i).tag.equals(constant)) {
                                return editedItems.get(i);
                            }
                        }
                        return null;
                    }
                }));

        final ViewGroup rootView = findViewById(R.id.root_view);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            rootView.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
                @Override
                @RequiresApi(api = Build.VERSION_CODES.KITKAT_WATCH)
                public WindowInsets onApplyWindowInsets(View view, WindowInsets insets) {
                    toolbar.setPadding(toolbar.getPaddingStart() + insets.getSystemWindowInsetLeft(),
                            toolbar.getPaddingTop() + insets.getSystemWindowInsetTop(),
                            toolbar.getPaddingEnd() + insets.getSystemWindowInsetRight(),
                            toolbar.getPaddingBottom());

                    recyclerView.setPadding(recyclerView.getPaddingStart() + insets.getSystemWindowInsetLeft(),
                            recyclerView.getPaddingTop(),
                            recyclerView.getPaddingEnd() + insets.getSystemWindowInsetRight(),
                            recyclerView.getPaddingBottom() + insets.getSystemWindowInsetBottom());

                    // clear this listener so insets aren't re-applied
                    rootView.setOnApplyWindowInsetsListener(null);
                    return insets.consumeSystemWindowInsets();
                }
            });
        } else {
            rootView.getViewTreeObserver()
                    .addOnGlobalLayoutListener(
                            new ViewTreeObserver.OnGlobalLayoutListener() {
                                @Override
                                public void onGlobalLayout() {
                                    //hacky way of getting window insets on pre-Lollipop
                                    int[] screenSize = Util.getScreenSize(ExifEditorActivity.this);

                                    int[] windowInsets = new int[]{
                                            Math.abs(screenSize[0] - rootView.getLeft()),
                                            Math.abs(screenSize[1] - rootView.getTop()),
                                            Math.abs(screenSize[2] - rootView.getRight()),
                                            Math.abs(screenSize[3] - rootView.getBottom())};

                                    toolbar.setPadding(toolbar.getPaddingStart() + windowInsets[0],
                                            toolbar.getPaddingTop() + windowInsets[1],
                                            toolbar.getPaddingEnd() + windowInsets[2],
                                            toolbar.getPaddingBottom());

                                    recyclerView.setPadding(recyclerView.getPaddingStart() + windowInsets[0],
                                            recyclerView.getPaddingTop(),
                                            recyclerView.getPaddingEnd() + windowInsets[2],
                                            recyclerView.getPaddingBottom() + windowInsets[3]);

                                    rootView.getViewTreeObserver()
                                            .removeOnGlobalLayoutListener(this);
                                }
                            });
        }

        //needed to achieve transparent statusBar in landscape; don't ask me why, but its working
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.exif_editor, menu);
        this.menu = menu;

        MenuItem save = menu.findItem(R.id.save);
        Drawable d = save.getIcon();
        DrawableCompat.wrap(d);
        DrawableCompat.setTint(d, textColorSecondary);
        save.setIcon(d);

        save.setVisible(editedItems.size() > 0);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                this.finish();
                break;
            case R.id.save:
                saveChanges();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    public void saveChanges() {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                boolean successful = true;

                for (int i = 0; i < editedItems.size(); i++) {
                    EditedItem item = editedItems.get(i);
                    try {
                        String newValue = item.getCastNewValue();
                        exifInterface.setAttribute(item.tag, newValue);
                    } catch (NumberFormatException | NullPointerException e) {
                        e.printStackTrace();
                        successful = false;
                    }
                }

                try {
                    exifInterface.saveAttributes();
                } catch (IOException e) {
                    e.printStackTrace();
                    successful = false;
                }
                final int stringRes = successful ? R.string.changes_saved : R.string.error;
                ExifEditorActivity.this.runOnUiThread(
                        new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(ExifEditorActivity.this, stringRes, Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        });
    }


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putParcelableArrayList(EDITED_ITEMS, editedItems);
    }

    @Override
    public int getDarkThemeRes() {
        return R.style.Theme_CameraRoll_ExifEditor;
    }

    @Override
    public int getLightThemeRes() {
        return R.style.Theme_CameraRoll_Light_ExifEditor;
    }

    @Override
    public void onThemeApplied(Theme theme) {
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setBackgroundColor(toolbarColor);
        toolbar.setTitleTextColor(textColorPrimary);

        if (theme.darkStatusBarIcons() &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Util.setDarkStatusBarIcons(findViewById(R.id.root_view));
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            int statusBarColor = getStatusBarColor();
            getWindow().setStatusBarColor(statusBarColor);
        }
    }

    private static class EditedItem implements Parcelable {
        String tag;
        String newValue;

        EditedItem(String constant, String newValue) {
            this.tag = constant;
            this.newValue = newValue;
        }

        EditedItem(Parcel in) {
            tag = in.readString();
            newValue = in.readString();
        }

        void setNewValue(String newValue) {
            this.newValue = newValue;
        }

        String getCastNewValue() throws NumberFormatException, NullPointerException {
            return String.valueOf(ExifUtil.castValue(tag, newValue));
        }

        public static final Creator<EditedItem> CREATOR = new Creator<EditedItem>() {
            @Override
            public EditedItem createFromParcel(Parcel in) {
                return new EditedItem(in);
            }

            @Override
            public EditedItem[] newArray(int size) {
                return new EditedItem[size];
            }
        };

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            parcel.writeString(tag);
            parcel.writeString(newValue);
        }
    }

    private static class RecyclerViewAdapter extends RecyclerView.Adapter {

        interface OnEditCallback {
            void onItemEdited(String tag, String newValue);

            EditedItem getEditedItem(String tag);
        }

        static class ExifViewHolder extends RecyclerView.ViewHolder {

            private TextWatcher textWatcher;

            ExifViewHolder(View itemView) {
                super(itemView);
            }

            void setTextWatcher(TextWatcher textWatcher) {
                this.textWatcher = textWatcher;
            }

            TextWatcher getTextWatcher() {
                return textWatcher;
            }
        }

        private ExifInterface exifInterface;

        private OnEditCallback callback;

        RecyclerViewAdapter(ExifInterface exifInterface, OnEditCallback callback) {
            this.exifInterface = exifInterface;
            this.callback = callback;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.exif_editor_item, parent, false);
            return new ExifViewHolder(v);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            final String tag = ExifUtil.getExifTags()[position];

            TextView tagTV = holder.itemView.findViewById(R.id.tag);
            tagTV.setText(tag);

            final EditText value = holder.itemView.findViewById(R.id.value);
            EditedItem editedItem = callback.getEditedItem(tag);

            value.removeTextChangedListener(((ExifViewHolder) holder).getTextWatcher());
            value.setText(editedItem == null ? exifInterface.getAttribute(tag) : editedItem.newValue);

            ((ExifViewHolder) holder).setTextWatcher(new TextWatcher() {
                public void afterTextChanged(Editable s) {
                }

                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    callback.onItemEdited(tag, s.toString());
                }
            });
            value.addTextChangedListener(((ExifViewHolder) holder).getTextWatcher());
        }

        @Override
        public int getItemCount() {
            return ExifUtil.getExifTags().length;
        }
    }
}
