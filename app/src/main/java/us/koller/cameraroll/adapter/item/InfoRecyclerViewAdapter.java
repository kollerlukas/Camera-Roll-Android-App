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
import android.support.media.ExifInterface;
import android.support.v4.content.ContextCompat;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.SimpleTarget;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import us.koller.cameraroll.R;
import us.koller.cameraroll.data.AlbumItem;
import us.koller.cameraroll.data.Gif;
import us.koller.cameraroll.data.Photo;
import us.koller.cameraroll.util.ExifUtil;
import us.koller.cameraroll.util.InfoUtil;
import us.koller.cameraroll.util.MediaType;
import us.koller.cameraroll.util.Util;

import static android.content.Context.CLIPBOARD_SERVICE;

public class InfoRecyclerViewAdapter extends RecyclerView.Adapter {

    private static final int INFO_VIEW_TYPE = 0;
    private static final int COLOR_VIEW_TYPE = 1;
    private static final int LOCATION_VIEW_TYPE = 2;

    public interface OnDataRetrievedCallback {
        void onDataRetrieved();

        Context getContext();
    }

    private static class InfoItem {
        private String type, value;

        InfoItem(String type, String value) {
            this.type = type;
            this.value = value;
        }

        String getType() {
            return type;
        }

        String getValue() {
            return value;
        }
    }

    private static class ColorsItem extends InfoItem {

        private String path;

        ColorsItem(String path) {
            super("Colors", null);
            this.path = path;
        }
    }

    private static class LocationItem extends InfoItem {

        LocationItem(String type, String value) {
            super(type, value);
        }
    }

    private ArrayList<InfoItem> infoItems;

    public boolean exifSupported(Context context, AlbumItem albumItem) {
        String mimeType = MediaType.getMimeType(context, albumItem.getUri(context));
        return mimeType != null && MediaType.doesSupportExif_MimeType(mimeType);
    }

    public void retrieveData(final AlbumItem albumItem, final boolean showColors, final OnDataRetrievedCallback callback) {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                infoItems = new ArrayList<>();
                if (showColors) {
                    infoItems.add(new ColorsItem(albumItem.getPath()));
                }

                Context context = callback.getContext();

                File file = new File(albumItem.getPath());
                Uri uri = albumItem.getUri(context);

                String name = albumItem.getName();
                infoItems.add(new InfoItem(context.getString(R.string.info_filename), name));

                String path = file.getPath();
                infoItems.add(new InfoItem(context.getString(R.string.info_filepath), path));

                String size = InfoUtil.retrieveFileSize(context, uri);
                if (size == null) {
                    size = InfoUtil.parseFileSize(0);
                }
                infoItems.add(new InfoItem(context.getString(R.string.info_size), size));

                /*locale needed for date formatting*/
                Locale locale = Util.getLocale(context);

                if (exifSupported(context, albumItem)) {
                    ExifInterface exif = ExifUtil.getExifInterface(context, albumItem);

                    /*Dimensions*/
                    String height = String.valueOf(ExifUtil.getCastValue(exif, ExifInterface.TAG_IMAGE_LENGTH));
                    String width = String.valueOf(ExifUtil.getCastValue(exif, ExifInterface.TAG_IMAGE_WIDTH));
                    infoItems.add(new InfoItem(context.getString(R.string.info_dimensions), width + " x " + height));

                    /*Date*/
                    String dateString = String.valueOf(ExifUtil.getCastValue(exif, ExifInterface.TAG_DATETIME));
                    try {
                        Date date = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss", locale).parse(dateString);
                        String formattedDate = new SimpleDateFormat("EEE, d MMM yyyy HH:mm", locale).format(date);
                        infoItems.add(new InfoItem(context.getString(R.string.info_date), formattedDate));
                    } catch (ParseException e) {
                        e.printStackTrace();
                        String formattedDate = new SimpleDateFormat("EEE, d MMM yyyy HH:mm", locale)
                                .format(new Date(albumItem.getDate()));
                        infoItems.add(new InfoItem(context.getString(R.string.info_date), formattedDate));
                    }

                    /*Location*/
                    LocationItem locationItem;
                    Object latitudeObject = ExifUtil.getCastValue(exif, ExifInterface.TAG_GPS_LATITUDE);
                    Object longitudeObject = ExifUtil.getCastValue(exif, ExifInterface.TAG_GPS_LONGITUDE);
                    if (latitudeObject != null && longitudeObject != null) {
                        boolean positiveLat = ExifUtil.getCastValue(exif, ExifInterface.TAG_GPS_LATITUDE_REF).equals("N");
                        double latitude = Double.parseDouble(InfoUtil.parseGPSLongOrLat(String.valueOf(latitudeObject), positiveLat));

                        boolean positiveLong = ExifUtil.getCastValue(exif, ExifInterface.TAG_GPS_LONGITUDE_REF).equals("E");
                        double longitude = Double.parseDouble(InfoUtil.parseGPSLongOrLat(String.valueOf(longitudeObject), positiveLong));
                        String locationString = latitude + "," + longitude;

                        locationItem = new LocationItem(context.getString(R.string.info_location), locationString);
                    } else {
                        locationItem = new LocationItem(context.getString(R.string.info_location), ExifUtil.NO_DATA);
                    }
                    infoItems.add(locationItem);

                    /*Focal Length*/
                    Object focalLengthObject = ExifUtil.getCastValue(exif, ExifInterface.TAG_FOCAL_LENGTH);
                    String focalLength;
                    if (focalLengthObject != null) {
                        focalLength = String.valueOf(focalLengthObject);
                    } else {
                        focalLength = ExifUtil.NO_DATA;
                    }
                    infoItems.add(new InfoItem(context.getString(R.string.info_focal_length), focalLength));

                    /*Exposure*/
                    Object exposureObject = String.valueOf(ExifUtil.getCastValue(exif, ExifInterface.TAG_EXPOSURE_TIME));
                    String exposure;
                    if (exposureObject != null) {
                        exposure = InfoUtil.parseExposureTime(String.valueOf(exposureObject));
                    } else {
                        exposure = ExifUtil.NO_DATA;
                    }
                    infoItems.add(new InfoItem(context.getString(R.string.info_exposure), exposure));

                    /*Model & Make*/
                    Object makeObject = ExifUtil.getCastValue(exif, ExifInterface.TAG_MAKE);
                    Object modelObject = ExifUtil.getCastValue(exif, ExifInterface.TAG_MODEL);
                    String model;
                    if (makeObject != null && modelObject != null) {
                        model = String.valueOf(makeObject) + " " + String.valueOf(modelObject);
                    } else {
                        model = ExifUtil.NO_DATA;
                    }
                    infoItems.add(new InfoItem(context.getString(R.string.info_camera_model), model));

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        /*Aperture*/
                        Object apertureObject = ExifUtil.getCastValue(exif, ExifInterface.TAG_F_NUMBER);
                        String aperture;
                        if (apertureObject != null) {
                            aperture = "f/" + String.valueOf(apertureObject);
                        } else {
                            aperture = ExifUtil.NO_DATA;
                        }
                        infoItems.add(new InfoItem(context.getString(R.string.info_aperture), aperture));

                        /*ISO*/
                        Object isoObject = ExifUtil.getCastValue(exif, ExifInterface.TAG_ISO_SPEED_RATINGS);
                        String iso;
                        if (apertureObject != null) {
                            iso = String.valueOf(isoObject);
                        } else {
                            iso = ExifUtil.NO_DATA;
                        }
                        infoItems.add(new InfoItem(context.getString(R.string.info_iso), iso));
                    }
                } else {
                    /*Exif not supported/working for this image*/
                    int[] imageDimens = albumItem.getImageDimens(context);
                    String height = String.valueOf(imageDimens[1]);
                    String width = String.valueOf(imageDimens[0]);
                    infoItems.add(new InfoItem(context.getString(R.string.info_dimensions), width + " x " + height));

                    String date = new SimpleDateFormat("EEE, d MMM yyyy HH:mm", locale)
                            .format(new Date(albumItem.getDate()));
                    infoItems.add(new InfoItem(context.getString(R.string.info_date), date));
                }

                callback.onDataRetrieved();
            }
        });
    }

    @Override
    public int getItemViewType(int position) {
        InfoItem infoItem = infoItems.get(position);
        if (infoItem instanceof ColorsItem) {
            return COLOR_VIEW_TYPE;
        } else if (infoItem instanceof LocationItem) {
            return LOCATION_VIEW_TYPE;
        }
        return INFO_VIEW_TYPE;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        int layoutRes = viewType == COLOR_VIEW_TYPE ? R.layout.info_color : R.layout.info_item;
        View v = LayoutInflater.from(parent.getContext()).inflate(layoutRes, parent, false);
        switch (viewType) {
            case INFO_VIEW_TYPE:
                return new InfoHolder(v);
            case COLOR_VIEW_TYPE:
                return new ColorHolder(v);
            case LOCATION_VIEW_TYPE:
                return new LocationHolder(v);
        }
        return null;
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, int position) {
        InfoItem infoItem = infoItems.get(position);
        if (holder instanceof ColorHolder && infoItem instanceof ColorsItem) {
            ((ColorHolder) holder).setColors((ColorsItem) infoItem);
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

        InfoHolder(View itemView) {
            super(itemView);
            type = itemView.findViewById(R.id.tag);
            value = itemView.findViewById(R.id.value);
        }

        void bind(InfoItem infoItem) {
            type.setText(infoItem.getType());
            value.setText(infoItem.getValue());
        }
    }

    static class LocationHolder extends InfoHolder {

        private LocationItem locationItem;

        private String featureName;

        LocationHolder(View itemView) {
            super(itemView);
        }

        @Override
        void bind(InfoItem infoItem) {
            type.setText(infoItem.getType());
            if (infoItem instanceof LocationItem) {
                locationItem = (LocationItem) infoItem;
                value.setText(locationItem.getValue());
                retrieveAddress(itemView.getContext(), locationItem.getValue());

                if (!locationItem.getValue().equals(ExifUtil.NO_DATA)) {
                    value.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            launchLocation();
                        }
                    });
                } else {
                    value.setOnClickListener(null);
                }
            }
        }

        private void retrieveAddress(final Context context, final String locationString) {
            if (!Util.hasWifiConnection(context)) {
                return;
            }

            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    String valueText = locationItem.getValue();
                    String[] parts = locationString.split(",");
                    try {
                        double lat = Double.parseDouble(parts[0]);
                        double lng = Double.parseDouble(parts[1]);

                        Address address = InfoUtil.retrieveAddredd(context, lat, lng);
                        if (address != null) {
                            featureName = address.getFeatureName();
                            valueText = address.getLocality() + ", " + address.getAdminArea();
                        }
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                    }

                    final String finalValueText = valueText;
                    value.post(new Runnable() {
                        @Override
                        public void run() {
                            value.setText(finalValueText);
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
            Glide.with(itemView.getContext())
                    .asBitmap()
                    .load(uri)
                    .into(new SimpleTarget<Bitmap>() {
                        @Override
                        public void onResourceReady(Bitmap bitmap, com.bumptech.glide.request
                                .transition.Transition<? super Bitmap> transition) {
                            // Do something with bitmap here.
                            Palette.from(bitmap).generate(new Palette.PaletteAsyncListener() {
                                @Override
                                public void onGenerated(Palette palette) {
                                    p = palette;
                                    setColors(null);
                                }
                            });
                        }
                    });
        }

        private void setColors(ColorsItem colorsItem) {
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
                int transparent = ContextCompat.getColor(card.getContext(),
                        android.R.color.transparent);
                card.setCardBackgroundColor(transparent);
                text.setText("N/A");
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
                return ContextCompat.getColor(context, R.color.white_translucent1);
            }
            return ContextCompat.getColor(context, R.color.grey_900_translucent);
        }
    }
}