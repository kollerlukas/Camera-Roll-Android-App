package us.koller.cameraroll;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import us.koller.cameraroll.data.models.Album;
import us.koller.cameraroll.data.models.AlbumItem;
import us.koller.cameraroll.data.models.Video;
import us.koller.cameraroll.ui.EditImageActivity;
import us.koller.cameraroll.ui.ItemActivity;
import us.koller.cameraroll.ui.MainActivity;
import us.koller.cameraroll.ui.VideoPlayerActivity;

public class IntentReceiver extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        switch (getIntent().getAction()) {
            case "com.android.camera.action.REVIEW":
            case Intent.ACTION_VIEW:
                view(getIntent());
                this.finish();
                break;
            case Intent.ACTION_PICK:
                pick(getIntent());
                break;
            case Intent.ACTION_GET_CONTENT:
                pick(getIntent());
                break;
            case Intent.ACTION_EDIT:
                edit(getIntent());
                break;
            default:
                break;
        }
    }

    private void view(Intent intent) {
        Uri uri = intent.getData();
        if (uri == null) {
            Toast.makeText(this, getString(R.string.error) + ": Uri = null", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, MainActivity.class));
            this.finish();
            return;
        }

        Album album = new Album().setPath("");
        AlbumItem albumItem;
        String mimeType = intent.getType();
        if (mimeType != null) {
            albumItem = AlbumItem.getInstance(this, uri, mimeType);
        } else {
            albumItem = AlbumItem.getInstance(this, uri);
        }

        if (albumItem == null) {
            Toast.makeText(this, getString(R.string.error), Toast.LENGTH_SHORT).show();
            this.finish();
            return;
        }

        album.getAlbumItems().add(albumItem);

        if (albumItem instanceof Video) {
            Intent view_video = new Intent(this, VideoPlayerActivity.class)
                    .setData(uri);
            startActivity(view_video);
        } else {
            Intent view_photo = new Intent(this, ItemActivity.class)
                    .setData(uri)
                    .putExtra(ItemActivity.ALBUM_ITEM, albumItem)
                    .putExtra(ItemActivity.VIEW_ONLY, true)
                    .putExtra(ItemActivity.ALBUM, album)
                    .putExtra(ItemActivity.ITEM_POSITION, album.getAlbumItems().indexOf(albumItem))
                    .addFlags(intent.getFlags());
            startActivity(view_photo);
        }
        this.finish();
    }

    private void pick(Intent intent) {
        setIntent(new Intent("ACTIVITY_ALREADY_LAUNCHED"));

        Intent pick_photos = new Intent(this, MainActivity.class)
                .putExtra(Intent.EXTRA_ALLOW_MULTIPLE, intent.getBooleanExtra(Intent.EXTRA_ALLOW_MULTIPLE, false))
                .setAction(MainActivity.PICK_PHOTOS);

        startActivityForResult(pick_photos, MainActivity.PICK_PHOTOS_REQUEST_CODE);
    }

    private void edit(Intent intent) {
        String imagePath = intent.getStringExtra(EditImageActivity.IMAGE_PATH);

        Intent edit = new Intent(this, EditImageActivity.class)
                .setAction(Intent.ACTION_EDIT)
                .setDataAndType(intent.getData(), intent.getType())
                .putExtra(EditImageActivity.IMAGE_PATH, imagePath);

        startActivity(edit);
        this.finish();
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
            default:
                break;
        }
    }
}
