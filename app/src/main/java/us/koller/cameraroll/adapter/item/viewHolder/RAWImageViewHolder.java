package us.koller.cameraroll.adapter.item.viewHolder;

import com.davemorrissey.labs.subscaleview.decoder.ImageRegionDecoder;

import us.koller.cameraroll.data.AlbumItem;
import us.koller.cameraroll.imageDecoder.RAWImageBitmapRegionDecoder;

public class RAWImageViewHolder extends PhotoViewHolder {

    public RAWImageViewHolder(AlbumItem albumItem, int position) {
        super(albumItem, position);
    }

    @Override
    public Class<? extends ImageRegionDecoder> getBitmapRegionDecoderClass() {
        return RAWImageBitmapRegionDecoder.class;
    }

}
