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
import us.koller.cameraroll.ui.MainActivity;

public class IntentReceiver extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        switch (getIntent().getAction()) {
            case Intent.ACTION_VIEW:
                viewPhoto(getIntent());
                this.finish();
                break;
            case "com.android.camera.action.REVIEW":
                viewPhoto(getIntent());
                this.finish();
                break;
            case Intent.ACTION_PICK:
                pickPhoto(getIntent());
                break;
            case Intent.ACTION_GET_CONTENT:
                pickPhoto(getIntent());
                break;
        }
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

        Intent view_photo = new Intent(this, ItemActivity.class)
                .putExtra(ItemActivity.ALBUM_ITEM, albumItem)
                .putExtra(ItemActivity.VIEW_ONLY, true)
                .putExtra(ItemActivity.ALBUM, album)
                .putExtra(ItemActivity.ITEM_POSITION, album.getAlbumItems().indexOf(albumItem))
                .addFlags(intent.getFlags());
        startActivity(view_photo);
    }

    public void pickPhoto(Intent intent) {
        setIntent(new Intent("ACTIVITY_ALREADY_LAUNCHED"));

        Intent pick_photos = new Intent(this, MainActivity.class)
                .putExtra(Intent.EXTRA_ALLOW_MULTIPLE, intent.getBooleanExtra(Intent.EXTRA_ALLOW_MULTIPLE, false))
                .setAction(MainActivity.PICK_PHOTOS);

        startActivityForResult(pick_photos, MainActivity.PICK_PHOTOS_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case MainActivity.PICK_PHOTOS_REQUEST_CODE:
                if (resultCode != RESULT_CANCELED) {
                    setResult(RESULT_OK, data);
                }
                this.finish();
                break;
        }
    }
}
