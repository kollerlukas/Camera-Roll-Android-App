package us.koller.cameraroll;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import us.koller.cameraroll.ui.ItemActivity;
import us.koller.cameraroll.data.Album;
import us.koller.cameraroll.data.MediaLoader;

public class IntentReceiver extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        switch (getIntent().getAction()) {
            case Intent.ACTION_VIEW:
                viewPhoto(getIntent());
                break;
            case Intent.ACTION_GET_CONTENT:
                pickPhoto(getIntent());
                break;
        }

        this.finish();
    }

    public void viewPhoto(Intent intent) {
        Uri uri = intent.getData();
        if (uri == null) {
            Toast.makeText(this, getString(R.string.error) + ": Uri = null", Toast.LENGTH_SHORT).show();
            this.finish();
            return;
        }

        Album album = new Album();
        Album.Photo photo = new MediaLoader().loadPhoto(this, uri);
        if (photo == null) {
            Toast.makeText(this, getString(R.string.error), Toast.LENGTH_SHORT).show();
            this.finish();
            return;
        }
        album.getAlbumItems().add(photo);

        startActivity(new Intent(this, ItemActivity.class)
                .putExtra(ItemActivity.ALBUM_ITEM, photo)
                .putExtra(ItemActivity.VIEW_ONLY, true)
                .putExtra(ItemActivity.ALBUM, album)
                .putExtra(ItemActivity.ITEM_POSITION, album.getAlbumItems().indexOf(photo)));
    }

    public void pickPhoto(Intent intent) {
        Toast.makeText(this, "pickPhoto()", Toast.LENGTH_SHORT).show();
    }
}
