package us.koller.cameraroll.adapter.fileExplorer.ViewHolder;

import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;

import us.koller.cameraroll.R;
import us.koller.cameraroll.data.File_POJO;
import us.koller.cameraroll.data.StorageRoot;
import us.koller.cameraroll.util.MediaType;

public class FileHolder extends RecyclerView.ViewHolder {

    private File_POJO file;

    public FileHolder(View itemView) {
        super(itemView);
    }

    public void setFile(File_POJO file) {
        this.file = file;

        ImageView folderIndicator = (ImageView) itemView.findViewById(R.id.folder_indicator);
        if (file instanceof StorageRoot) {
            if (file.getName().equals(itemView.getContext().getString(R.string.storage))) {
                folderIndicator.setImageResource(R.drawable.ic_smartphone_white_24dp);
            } else {
                folderIndicator.setImageResource(R.drawable.ic_sd_storage_white_24dp);
            }
        } else if (!file.isMedia) {
            if (new File(file.getPath()).isFile()) {
                folderIndicator.setImageResource(R.drawable.ic_insert_drive_file_white_24dp);
            } else {
                folderIndicator.setImageResource(R.drawable.ic_folder_white_24dp);
            }
        } else if (MediaType.isVideo(folderIndicator.getContext(), file.getPath())) {
            folderIndicator.setImageResource(R.drawable.ic_videocam_white_24dp);
        } else {
            folderIndicator.setImageResource(R.drawable.ic_photo_white_24dp);
        }

        TextView textView = (TextView) itemView.findViewById(R.id.text);
        textView.setText(file.getName());
    }

    public void setSelected(boolean selected) {
        itemView.setBackgroundColor(
                ContextCompat.getColor(itemView.getContext(),
                        selected ? R.color.colorAccent_translucent :
                                android.R.color.transparent));

        TextView textView = (TextView) itemView.findViewById(R.id.text);
        textView.setTextColor(ContextCompat.getColor(itemView.getContext(),
                selected ? R.color.grey_900_translucent : R.color.white_translucent1));

        ImageView folderIndicator = (ImageView) itemView.findViewById(R.id.folder_indicator);
        Drawable d = folderIndicator.getDrawable();
        /*d.setTint(ContextCompat.getColor(itemView.getContext(),
                selected ? R.color.grey_900_translucent : R.color.white));*/
        d = DrawableCompat.wrap(d);
        DrawableCompat.setTint(d.mutate(), ContextCompat.getColor(itemView.getContext(),
                selected ? R.color.grey_900_translucent : R.color.white));
        folderIndicator.setImageDrawable(d);
    }
}
