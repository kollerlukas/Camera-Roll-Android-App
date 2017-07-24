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

import java.util.ArrayList;

import us.koller.cameraroll.R;
import us.koller.cameraroll.data.AlbumItem;
import us.koller.cameraroll.data.Gif;
import us.koller.cameraroll.data.Photo;
import us.koller.cameraroll.data.Video;
import us.koller.cameraroll.util.ExifUtil;
import us.koller.cameraroll.util.InfoUtil;
import us.koller.cameraroll.util.MediaType;
import us.koller.cameraroll.util.Util;

import static android.content.Context.CLIPBOARD_SERVICE;

public class InfoRecyclerViewAdapter extends RecyclerView.Adapter {

    private static final int INFO_VIEW_TYPE = 0;
    private static final int COLOR_VIEW_TYPE = 1;
    private static final int LOCATION_VIEW_TYPE = 2;

    private ArrayList<InfoUtil.InfoItem> infoItems;

    public interface OnDataRetrievedCallback {
        void onDataRetrieved();

        Context getContext();
    }

    public boolean exifSupported(Context context, AlbumItem albumItem) {
        String mimeType = MediaType.getMimeType(context, albumItem.getUri(context));
        return mimeType != null && MediaType.doesSupportExifMimeType(mimeType);
    }

    public void retrieveData(final AlbumItem albumItem, final boolean showColors, final OnDataRetrievedCallback callback) {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                infoItems = new ArrayList<>();
                if (showColors) {
                    infoItems.add(new InfoUtil.ColorsItem(albumItem.getPath()));
                }

                Context context = callback.getContext();

                Uri uri = albumItem.getUri(context);

                infoItems.add(new InfoUtil.InfoItem(context.getString(R.string.info_filename), albumItem.getName()));
                infoItems.add(new InfoUtil.InfoItem(context.getString(R.string.info_filepath), albumItem.getPath()));
                infoItems.add(InfoUtil.retrieveFileSize(context, uri));

                ExifInterface exif = null;
                if (exifSupported(context, albumItem)) {
                    exif = ExifUtil.getExifInterface(context, albumItem);
                }

                infoItems.add(InfoUtil.retrieveDimensions(context, exif, albumItem));
                infoItems.add(InfoUtil.retrieveFormattedDate(context, exif, albumItem));

                if (exif != null) {
                    infoItems.add(InfoUtil.retrieveLocation(context, exif));
                    infoItems.add(InfoUtil.retrieveFocalLength(context, exif));
                    infoItems.add(InfoUtil.retrieveExposure(context, exif));
                    infoItems.add(InfoUtil.retrieveModelAndMake(context, exif));

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        infoItems.add(InfoUtil.retrieveAperture(context, exif));
                        infoItems.add(InfoUtil.retrieveISO(context, exif));
                    }
                }

                if (albumItem instanceof Video) {
                    infoItems.add(InfoUtil.retrieveVideoFrameRate(context, albumItem));
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
            default:
                break;
        }
        return null;
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, int position) {
        InfoUtil.InfoItem infoItem = infoItems.get(position);
        if (holder instanceof ColorHolder && infoItem instanceof InfoUtil.ColorsItem) {
            ((ColorHolder) holder).setColors((InfoUtil.ColorsItem) infoItem);
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

        void bind(InfoUtil.InfoItem infoItem) {
            type.setText(infoItem.getType());
            value.setText(infoItem.getValue());
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
            if (infoItem instanceof InfoUtil.LocationItem) {
                locationItem = (InfoUtil.LocationItem) infoItem;
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

                        Address address = InfoUtil.retrieveAddress(context, lat, lng);
                        if (address != null) {
                            featureName = address.getFeatureName();
                            valueText = address.getLocality() + ", " + address.getAdminArea();
                        }
                    } catch (NumberFormatException ignored) {
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