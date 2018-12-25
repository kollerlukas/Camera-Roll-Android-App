package us.koller.cameraroll.adapter.item;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.location.Address;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.media.ExifInterface;
import android.support.v4.content.ContextCompat;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.SimpleTarget;

import java.util.ArrayList;
import java.util.List;

import us.koller.cameraroll.R;
import us.koller.cameraroll.data.Settings;
import us.koller.cameraroll.data.models.AlbumItem;
import us.koller.cameraroll.data.models.Gif;
import us.koller.cameraroll.data.models.Photo;
import us.koller.cameraroll.data.models.Video;
import us.koller.cameraroll.themes.Theme;
import us.koller.cameraroll.util.ExifUtil;
import us.koller.cameraroll.util.InfoUtil;
import us.koller.cameraroll.util.MediaType;
import us.koller.cameraroll.util.Util;

import static android.content.Context.CLIPBOARD_SERVICE;

public class InfoRecyclerViewAdapter extends RecyclerView.Adapter {

    private static final int INFO_VIEW_TYPE = 0;
    private static final int COLOR_VIEW_TYPE = 1;
    private static final int LOCATION_VIEW_TYPE = 2;
    private static final int TAGS_VIEW_TYPE = 3;

    private ArrayList<InfoUtil.InfoItem> infoItems;

    public interface OnDataRetrievedCallback {
        void onDataRetrieved();

        void failed();

        Context getContext();
    }

    public boolean exifSupported(Context context, AlbumItem albumItem) {
        String mimeType = MediaType.getMimeType(context, albumItem.getUri(context));
        return mimeType != null && MediaType.doesSupportExifMimeType(mimeType);
    }

    public void retrieveData(final AlbumItem albumItem, final boolean showColors, final OnDataRetrievedCallback callback) {
        if (albumItem == null) {
            callback.failed();
            return;
        }

        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                infoItems = new ArrayList<>();
                if (showColors) {
                    infoItems.add(new InfoUtil.ColorsItem(albumItem.getPath()));
                }

                //infoItems.add(new InfoUtil.TagsItem(albumItem));

                Context context = callback.getContext();

                Uri uri = albumItem.getUri(context);

                infoItems.add(new InfoUtil.InfoItem(context.getString(R.string.info_filename), albumItem.getName())
                        .setIconRes(R.drawable.ic_insert_drive_file_white));
                infoItems.add(new InfoUtil.InfoItem(context.getString(R.string.info_filepath), albumItem.getPath())
                        .setIconRes(R.drawable.ic_folder_white));
                infoItems.add(InfoUtil.retrieveFileSize(context, uri).setIconRes(R.drawable.ic_memory_white));

                ExifInterface exif = null;
                if (exifSupported(context, albumItem)) {
                    exif = ExifUtil.getExifInterface(context, albumItem);
                }

                infoItems.add(InfoUtil.retrieveDimensions(context, exif, albumItem)
                        .setIconRes(R.drawable.ic_fullscreen_white));
                infoItems.add(InfoUtil.retrieveFormattedDate(context, exif, albumItem)
                        .setIconRes(R.drawable.ic_date_range_white));

                if (exif != null) {
                    infoItems.add(InfoUtil.retrieveLocation(context, exif)
                            .setIconRes(R.drawable.ic_location_on_white));
                    infoItems.add(InfoUtil.retrieveFocalLength(context, exif)
                            .setIconRes(R.drawable.ic_straighten_white));
                    infoItems.add(InfoUtil.retrieveExposure(context, exif)
                            .setIconRes(R.drawable.ic_timelapse_white));
                    infoItems.add(InfoUtil.retrieveModelAndMake(context, exif)
                            .setIconRes(R.drawable.ic_camera_alt_white));

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        infoItems.add(InfoUtil.retrieveAperture(context, exif)
                                .setIconRes(R.drawable.ic_camera_white));
                        infoItems.add(InfoUtil.retrieveISO(context, exif)
                                .setIconRes(R.drawable.ic_iso_white));
                    }
                }

                if (albumItem instanceof Video) {
                    infoItems.add(InfoUtil.retrieveVideoFrameRate(context, albumItem)
                            .setIconRes(R.drawable.ic_movie_creation_white));
                }

                callback.onDataRetrieved();
            }
        });
    }

    @Override
    public int getItemViewType(int position) {
        InfoUtil.InfoItem infoItem = infoItems.get(position);
        if (infoItem instanceof InfoUtil.ColorsItem) {
            return COLOR_VIEW_TYPE;
        } else if (infoItem instanceof InfoUtil.LocationItem) {
            return LOCATION_VIEW_TYPE;
        } else if (infoItem instanceof InfoUtil.TagsItem) {
            return TAGS_VIEW_TYPE;
        }
        return INFO_VIEW_TYPE;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layoutRes;
        switch (viewType) {
            case COLOR_VIEW_TYPE:
                layoutRes = R.layout.info_color;
                break;
            case TAGS_VIEW_TYPE:
                layoutRes = R.layout.info_tags;
                break;
            default:
                layoutRes = R.layout.info_item;
                break;
        }
        View v = LayoutInflater.from(parent.getContext())
                .inflate(layoutRes, parent, false);
        switch (viewType) {
            case INFO_VIEW_TYPE:
                return new InfoHolder(v);
            case COLOR_VIEW_TYPE:
                return new ColorHolder(v);
            case LOCATION_VIEW_TYPE:
                return new LocationHolder(v);
            case TAGS_VIEW_TYPE:
                return new TagsHolder(v);
            default:
                break;
        }
        return new InfoHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull final RecyclerView.ViewHolder holder, int position) {
        InfoUtil.InfoItem infoItem = infoItems.get(position);
        if (holder instanceof ColorHolder && infoItem instanceof InfoUtil.ColorsItem) {
            ((ColorHolder) holder).setColors((InfoUtil.ColorsItem) infoItem);
        } else if (holder instanceof TagsHolder && infoItem instanceof InfoUtil.TagsItem) {
            ((TagsHolder) holder).setTags((InfoUtil.TagsItem) infoItem);
        } else if (holder instanceof InfoHolder) {
            ((InfoHolder) holder).bind(infoItem);
        }
    }

    @Override
    public int getItemCount() {
        return infoItems.size();
    }


    /*ViewHolder classes*/
    static class InfoHolder extends RecyclerView.ViewHolder {

        TextView type, value;
        ImageView icon;

        InfoHolder(View itemView) {
            super(itemView);
            type = itemView.findViewById(R.id.tag);
            value = itemView.findViewById(R.id.value);
            icon = itemView.findViewById(R.id.icon);
            setTextColors();
        }

        void bind(InfoUtil.InfoItem infoItem) {
            type.setText(infoItem.getType());
            if (ExifUtil.NO_DATA.equals(infoItem.getValue())) {
                value.setText(R.string.unknown);
            } else {
                value.setText(infoItem.getValue());
            }
            setIcon(infoItem);
        }

        void setIcon(InfoUtil.InfoItem infoItem) {
            icon.setImageResource(infoItem.getIconRes());
        }

        void setTextColors() {
            Context context = type.getContext();
            Theme theme = Settings.getInstance(context).getThemeInstance(context);
            type.setTextColor(theme.getTextColorSecondary(context));
            value.setTextColor(theme.getTextColorPrimary(context));
            icon.setColorFilter(theme.getTextColorPrimary(context));
        }
    }

    static class LocationHolder extends InfoHolder {

        private InfoUtil.LocationItem locationItem;

        private String featureName;

        LocationHolder(View itemView) {
            super(itemView);
        }

        @Override
        public void bind(InfoUtil.InfoItem infoItem) {
            type.setText(infoItem.getType());
            setIcon(infoItem);
            if (infoItem instanceof InfoUtil.LocationItem) {
                locationItem = (InfoUtil.LocationItem) infoItem;

                if (!ExifUtil.NO_DATA.equals(locationItem.getValue())) {
                    value.setText(locationItem.getValue());
                    itemView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            launchLocation();
                        }
                    });
                    retrieveAddress(itemView.getContext(), locationItem.getValue());
                } else {
                    value.setText(R.string.unknown);
                    itemView.setOnClickListener(null);
                    itemView.setClickable(false);
                }
            }
        }

        private void retrieveAddress(final Context context, final String locationString) {
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    String valueText = locationItem.getValue();
                    String[] parts = locationString.split(",");
                    try {
                        double lat = Double.parseDouble(parts[0]);
                        double lng = Double.parseDouble(parts[1]);

                        Address address = InfoUtil.retrieveAddress(context, lat, lng);
                        if (address != null) {
                            featureName = address.getFeatureName();
                            valueText = null;
                            if (address.getLocality() != null) {
                                valueText = address.getLocality();
                            }
                            if (address.getAdminArea() != null) {
                                if (valueText != null) {
                                    valueText += ", " + address.getAdminArea();
                                } else {
                                    valueText = address.getAdminArea();
                                }
                            }
                            if (valueText == null) {
                                valueText = locationString;
                            }

                        }
                    } catch (NumberFormatException ignored) {
                    }

                    final String finalValueText = valueText;
                    value.post(new Runnable() {
                        @Override
                        public void run() {
                            if (ExifUtil.NO_DATA.equals(finalValueText)) {
                                value.setText(R.string.unknown);
                            } else {
                                value.setText(finalValueText);
                            }
                        }
                    });
                }
            });
        }

        private void launchLocation() {
            String location = "geo:0,0?q=" + locationItem.getValue();
            if (featureName != null) {
                location += "(" + featureName + ")";
            }
            Uri gmUri = Uri.parse(location);
            Intent intent = new Intent(Intent.ACTION_VIEW)
                    .setData(gmUri)
                    .setPackage("com.google.android.apps.maps");

            Context context = itemView.getContext();
            if (intent.resolveActivity(context.getPackageManager()) != null) {
                context.startActivity(intent);
            }
        }
    }

    static class ColorHolder extends RecyclerView.ViewHolder {

        private Palette p;
        private Uri uri;

        private View.OnClickListener onClickListener
                = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String color = (String) view.getTag();
                if (color != null) {
                    ClipboardManager clipboard = (ClipboardManager) view.getContext()
                            .getSystemService(CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText("label", color);
                    clipboard.setPrimaryClip(clip);

                    Toast.makeText(view.getContext(),
                            R.string.copied_to_clipboard,
                            Toast.LENGTH_SHORT).show();
                }
            }
        };

        ColorHolder(View itemView) {
            super(itemView);
        }

        private void retrieveColors(final Uri uri) {
            if (uri == null) {
                return;
            }

            int[] imageDimens = Util
                    .getImageDimensions(itemView.getContext(), uri);

            RequestOptions options = new RequestOptions()
                    .skipMemoryCache(true)
                    .override((int) (imageDimens[0] * 0.1f), (int) (imageDimens[1] * 0.1f))
                    .diskCacheStrategy(DiskCacheStrategy.NONE);

            Glide.with(itemView.getContext())
                    .asBitmap()
                    .load(uri)
                    .apply(options)
                    .into(new SimpleTarget<Bitmap>() {
                        @Override
                        public void onResourceReady(@NonNull final Bitmap bitmap, com.bumptech.glide.request
                                .transition.Transition<? super Bitmap> transition) {
                            // Do something with bitmap here.
                            Palette.from(bitmap).generate(new Palette.PaletteAsyncListener() {
                                @Override
                                public void onGenerated(@NonNull Palette palette) {
                                    p = palette;
                                    setColors(null);
                                }
                            });
                        }
                    });
        }

        private void setColors(InfoUtil.ColorsItem colorsItem) {
            if (p == null) {
                AlbumItem albumItem = AlbumItem.getInstance(colorsItem.path);

                if (albumItem instanceof Photo || albumItem instanceof Gif) {
                    uri = albumItem.getUri(itemView.getContext());
                    retrieveColors(uri);
                } else {
                    itemView.setVisibility(View.GONE);
                }
                return;
            }

            int defaultColor = Color.argb(0, 0, 0, 0);

                /*Vibrant color*/
            setColor((CardView) itemView.findViewById(R.id.vibrant_card),
                    (TextView) itemView.findViewById(R.id.vibrant_text),
                    p.getVibrantColor(defaultColor));

                /*Vibrant Dark color*/
            setColor((CardView) itemView.findViewById(R.id.vibrant_dark_card),
                    (TextView) itemView.findViewById(R.id.vibrant_dark_text),
                    p.getDarkVibrantColor(defaultColor));

                /*Vibrant Light color*/
            setColor((CardView) itemView.findViewById(R.id.vibrant_light_card),
                    (TextView) itemView.findViewById(R.id.vibrant_light_text),
                    p.getLightVibrantColor(defaultColor));

                /*Muted color*/
            setColor((CardView) itemView.findViewById(R.id.muted_card),
                    (TextView) itemView.findViewById(R.id.muted_text),
                    p.getMutedColor(defaultColor));

                /*Muted Dark color*/
            setColor((CardView) itemView.findViewById(R.id.muted_dark_card),
                    (TextView) itemView.findViewById(R.id.muted_dark_text),
                    p.getDarkMutedColor(defaultColor));

                /*Muted Light color*/
            setColor((CardView) itemView.findViewById(R.id.muted_light_card),
                    (TextView) itemView.findViewById(R.id.muted_light_text),
                    p.getLightMutedColor(defaultColor));
        }

        private void setColor(CardView card, TextView text, int color) {
            if (Color.alpha(color) == 0) {
                //color not found
                card.setVisibility(View.GONE);
                return;
            }

            card.setCardBackgroundColor(color);
            text.setTextColor(getTextColor(text.getContext(), color));
            String colorHex = String.format("#%06X", (0xFFFFFF & color));
            text.setText(colorHex);

            card.setTag(colorHex);
            card.setOnClickListener(onClickListener);
        }

        private static int getTextColor(Context context, int backgroundColor) {
            if ((Color.red(backgroundColor) +
                    Color.green(backgroundColor) +
                    Color.blue(backgroundColor)) / 3 < 100) {
                return ContextCompat.getColor(context, R.color.grey_300);
            }
            return ContextCompat.getColor(context, R.color.grey_900);
        }
    }

    static class TagsHolder extends RecyclerView.ViewHolder {

        interface TagCallback {
            void removeTag(String tag);

            void addTag(String tag);
        }

        private RecyclerView recyclerView;

        TagsHolder(View itemView) {
            super(itemView);
            recyclerView = itemView.findViewById(R.id.recyclerView);
            recyclerView.setLayoutManager(new LinearLayoutManager(itemView.getContext(),
                    LinearLayoutManager.HORIZONTAL, false));
        }

        private void setTags(InfoUtil.TagsItem tagsItem) {
            final AlbumItem albumItem = tagsItem.getItem();
            recyclerView.setAdapter(new TagsAdapter(itemView.getContext(), albumItem));
        }

        private static class TagsAdapter extends RecyclerView.Adapter<TagsAdapter.TagHolder> {

            private static final int TAG_VIEW_TYPE = 1;
            private static final int ADD_TAG_VIEW_TYPE = 2;

            private List<String> tags;

            private TagCallback callback;

            TagsAdapter(final Context context, final AlbumItem albumItem) {
                tags = albumItem.getTags(context);
                callback = new TagCallback() {
                    @Override
                    public void removeTag(String tag) {
                        int index = tags.indexOf(tag);
                        boolean success = albumItem.removeTag(context, tag);
                        if (success) {
                            notifyItemRemoved(index);
                        }
                    }

                    @Override
                    public void addTag(String tag) {
                        boolean success = albumItem.addTag(context, tag);
                        if (success) {
                            notifyItemInserted(tags.size() - 1);
                        }
                    }
                };
            }

            @Override
            public int getItemViewType(int position) {
                return position < tags.size() ? TAG_VIEW_TYPE : ADD_TAG_VIEW_TYPE;
            }

            @NonNull
            @Override
            public TagHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                int layoutRes = viewType == TAG_VIEW_TYPE ? R.layout.info_tag : R.layout.info_add_tag;
                View v = LayoutInflater.from(parent.getContext())
                        .inflate(layoutRes, parent, false);
                return viewType == TAG_VIEW_TYPE ? new TagHolder(v).setCallback(callback) : new AddTagHolder(v).setCallback(callback);
            }

            @Override
            public void onBindViewHolder(@NonNull TagHolder holder, int position) {
                if (position < tags.size()) {
                    holder.bind(tags.get(position));
                }
            }

            @Override
            public int getItemCount() {
                return tags.size() + 1;
            }

            static class TagHolder extends RecyclerView.ViewHolder
                    implements View.OnClickListener {

                TagCallback callback;

                String tag;

                TextView textView;
                ImageView tagButton;

                TagHolder(View itemView) {
                    super(itemView);
                    init();
                }

                void init() {
                    textView = itemView.findViewById(R.id.text_view);
                    tagButton = itemView.findViewById(R.id.tag_button);
                    setTextColor();
                    setButtonColor();
                }

                void bind(String tag) {
                    this.tag = tag;
                    textView.setText(tag);
                    tagButton.setOnClickListener(this);
                }

                void setTextColor() {
                    int color = getColor();
                    textView.setTextColor(color);
                }

                void setButtonColor() {
                    int color = getColor();
                    tagButton.setColorFilter(color);
                    tagButton.setAlpha(Color.alpha(color) / 255.0f);
                }

                int getColor() {
                    Context context = itemView.getContext();
                    Theme theme = Settings.getInstance(context).getThemeInstance(context);
                    return theme.getTextColorSecondary(context);
                }

                @Override
                public void onClick(View view) {
                    callback.removeTag(tag);
                }

                public TagHolder setCallback(TagCallback callback) {
                    this.callback = callback;
                    return this;
                }
            }

            static class AddTagHolder extends TagHolder {

                EditText editText;

                AddTagHolder(View itemView) {
                    super(itemView);
                }

                @Override
                void init() {
                    editText = itemView.findViewById(R.id.edit_text);
                    setTextColor();
                    editText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                        @Override
                        public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                            callback.addTag(textView.getText().toString());
                            textView.setText("");
                            return false;
                        }
                    });
                }

                @Override
                void setTextColor() {
                    int color = getColor();
                    editText.setTextColor(color);
                }
            }
        }
    }
}