package us.koller.cameraroll;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import us.koller.cameraroll.data.AlbumItem;
import us.koller.cameraroll.data.Video;
import us.koller.cameraroll.ui.ItemActivity;
import us.koller.cameraroll.data.Album;

public class IntentReceiver extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        switch (getIntent().getAction()) {
            case Intent.ACTION_VIEW:
                viewPhoto(getIntent());
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
        AlbumItem albumItem = AlbumItem.getInstance(this, uri.toString());
        if (albumItem == null || albumItem instanceof Video) {
            Toast.makeText(this, getString(R.string.error), Toast.LENGTH_SHORT).show();
            this.finish();
            return;
        }
        album.getAlbumItems().add(albumItem);

        startActivity(new Intent(this, ItemActivity.class)
                .putExtra(ItemActivity.ALBUM_ITEM, albumItem)
                .putExtra(ItemActivity.VIEW_ONLY, true)
                .putExtra(ItemActivity.ALBUM, album)
                .putExtra(ItemActivity.ITEM_POSITION, album.getAlbumItems().indexOf(albumItem)));
    }
}
