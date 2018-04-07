package us.koller.cameraroll.ui;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.media.ExifInterface;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatSpinner;
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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;

import us.koller.cameraroll.R;
import us.koller.cameraroll.data.Settings;
import us.koller.cameraroll.data.models.AlbumItem;
import us.koller.cameraroll.themes.Theme;
import us.koller.cameraroll.util.ExifUtil;
import us.koller.cameraroll.util.MediaType;
import us.koller.cameraroll.util.Util;

//simple Activity to edit the Exif-Data of images
public class ExifEditorActivity extends ThemeableActivity {

    public static final String ALBUM_ITEM = "ALBUM_ITEM";
    public static final String EDITED_ITEMS = "EDITED_ITEMS";

    private Menu menu;

    private ExifInterface exifInterface;

    private ArrayList<ExifUtil.ExifItem> editedItems;

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
        if (!MediaType.doesSupportWritingExifMimeType(mimeType)) {
            Toast.makeText(this, "Editing Exif values is only supported for JPEG images", Toast.LENGTH_SHORT).show();
            this.finish();
            return;
        }

        exifInterface = null;
        try {
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
        recyclerView.setAdapter(new RecyclerViewAdapter(
                new RecyclerViewAdapter.OnEditCallback() {
                    @Override
                    public void onItemEdited(String tag, String newValue) {
                        for (ExifUtil.ExifItem item : editedItems) {
                            if (item.getTag().equals(tag)) {
                                item.setValue(newValue);
                                showSaveButton(true);
                                return;
                            }
                        }
                        // ExifItem wasn't previously edited
                        ExifUtil.ExifItem item = getItem(tag);
                        item.setValue(newValue);
                        editedItems.add(item);
                        showSaveButton(true);
                    }

                    @Override
                    public ExifUtil.ExifItem getItem(String tag) {
                        for (ExifUtil.ExifItem item : editedItems) {
                            if (item.getTag().equals(tag)) {
                                return item;
                            }
                        }
                        return new ExifUtil.ExifItem(tag, exifInterface.getAttribute(tag));
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
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.exif_editor, menu);
        this.menu = menu;

        MenuItem save = menu.findItem(R.id.save);
        save.setVisible(editedItems.size() > 0);
        Drawable d = save.getIcon();
        DrawableCompat.wrap(d);
        DrawableCompat.setTint(d, textColorSecondary);
        DrawableCompat.unwrap(d);
        save.setIcon(d);
        return super.onCreateOptionsMenu(menu);
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
            case R.id.clear_exif_data:
                new AlertDialog.Builder(this)
                        .setTitle(R.string.clear_exif_data)
                        .setPositiveButton(R.string.remove, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                // remove all Exif data
                                ExifUtil.removeExifData(exifInterface, new ExifUtil.Callback() {
                                    @Override
                                    public void done(final boolean success) {
                                        ExifEditorActivity.this.runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                int stringRes = success ? R.string.changes_saved : R.string.error;
                                                Toast.makeText(ExifEditorActivity.this, stringRes, Toast.LENGTH_SHORT).show();
                                                RecyclerView recyclerView = findViewById(R.id.recyclerView);
                                                recyclerView.getAdapter().notifyDataSetChanged();
                                            }
                                        });
                                    }
                                });
                            }
                        })
                        .setNegativeButton(R.string.cancel, null)
                        .create().show();
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    public void showSaveButton(boolean show) {
        //make save button visible
        MenuItem save = menu.findItem(R.id.save);
        save.setVisible(show && editedItems.size() > 0);
    }

    public void saveChanges() {
        ExifUtil.saveChanges(exifInterface, editedItems, new ExifUtil.Callback() {
            @Override
            public void done(final boolean success) {
                ExifEditorActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ExifEditorActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                int stringRes = success ? R.string.changes_saved : R.string.error;
                                Toast.makeText(ExifEditorActivity.this, stringRes, Toast.LENGTH_SHORT).show();
                                if (success) {
                                    showSaveButton(false);
                                }
                            }
                        });
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
        return R.style.CameraRoll_Theme_ExifEditor;
    }

    @Override
    public int getLightThemeRes() {
        return R.style.CameraRoll_Theme_Light_ExifEditor;
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

    private static class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.ExifViewHolder> {

        private static final int VIEW_TYPE_EDIT_TEXT = 0;
        private static final int VIEW_TYPE_SPINNER = 1;

        private OnEditCallback callback;

        interface OnEditCallback {
            void onItemEdited(String tag, String newValue);

            ExifUtil.ExifItem getItem(String tag);
        }

        static class ExifViewHolder extends RecyclerView.ViewHolder {

            private TextWatcher textWatcher;

            ExifViewHolder(View itemView) {
                super(itemView);
                setTextColors();
            }

            void setTextWatcher(TextWatcher textWatcher) {
                this.textWatcher = textWatcher;
            }

            TextWatcher getTextWatcher() {
                return textWatcher;
            }

            void setTextColors() {
                Context context = itemView.getContext();
                TextView tagTV = itemView.findViewById(R.id.tag);
                EditText valueET = itemView.findViewById(R.id.value);
                Theme theme = Settings.getInstance(context).getThemeInstance(context);
                tagTV.setTextColor(theme.getTextColorSecondary(context));
                if (valueET != null) {
                    valueET.setTextColor(theme.getTextColorPrimary(context));
                }
            }
        }

        RecyclerViewAdapter(OnEditCallback callback) {
            this.callback = callback;
        }

        @Override
        public int getItemViewType(int position) {
            if (ExifUtil.getExifValues()[position] != null) {
                return VIEW_TYPE_SPINNER;
            }
            return VIEW_TYPE_EDIT_TEXT;
        }

        @NonNull
        @Override
        public ExifViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            int layoutRes;
            switch (viewType) {
                case VIEW_TYPE_EDIT_TEXT:
                    layoutRes = R.layout.exif_editor_item;
                    break;
                case VIEW_TYPE_SPINNER:
                    layoutRes = R.layout.exif_editor_spinner_item;
                    break;
                default:
                    layoutRes = -1;
                    break;
            }
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(layoutRes, parent, false);
            return new ExifViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ExifViewHolder holder, int position) {
            final String tag = ExifUtil.getExifTags()[position];

            TextView tagTV = holder.itemView.findViewById(R.id.tag);
            tagTV.setText(tag);

            ExifUtil.ExifItem item = callback.getItem(tag);

            if (ExifUtil.getExifValues()[position] != null) {
                final AppCompatSpinner spinner = holder.itemView.findViewById(R.id.value_spinner);
                String[] values = ExifUtil.getExifValues()[position];
                ArrayAdapter arrayAdapter = new ArrayAdapter<>(
                        holder.itemView.getContext(),
                        R.layout.simple_spinner_item,
                        values);
                arrayAdapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item);
                spinner.setAdapter(arrayAdapter);
                int selection = 0;
                try {
                    selection = Integer.parseInt(item.getValue());
                } catch (NumberFormatException ignored) {
                }
                final int startSelection = selection;
                spinner.setSelection(startSelection);
                spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                        if (startSelection != position) {
                            callback.onItemEdited(tag, String.valueOf(position));
                        }
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> adapterView) {
                    }
                });
            } else {
                final EditText value = holder.itemView.findViewById(R.id.value);
                value.removeTextChangedListener(holder.getTextWatcher());
                value.setText(item.getValue());

                holder.setTextWatcher(new SimpleTextWatcher() {
                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        callback.onItemEdited(tag, s.toString());
                    }
                });
                value.addTextChangedListener(holder.getTextWatcher());
            }
        }

        @Override
        public int getItemCount() {
            return ExifUtil.getExifTags().length;
        }
    }

    public static class SimpleTextWatcher implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

        }

        @Override
        public void afterTextChanged(Editable editable) {

        }
    }
}
