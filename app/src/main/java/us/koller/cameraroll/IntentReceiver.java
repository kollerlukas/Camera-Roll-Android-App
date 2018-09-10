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

    private void view(Intent i) {
        Uri uri = i.getData();
        if (uri == null) {
            Toast.makeText(this, getString(R.string.error) + ": Uri = null", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, MainActivity.class));
            this.finish();
            return;
        }

        Album a = new Album().setPath("");
        AlbumItem aI;
        String mimeType = i.getType();
        if (mimeType != null) {
            aI = AlbumItem.getInstance(this, uri, mimeType);
        } else {
            aI = AlbumItem.getInstance(this, uri);
        }

        if (aI == null) {
            Toast.makeText(this, getString(R.string.error), Toast.LENGTH_SHORT).show();
            this.finish();
            return;
        }
        a.getAlbumItems().add(aI);

        if (aI instanceof Video) {
            Intent view_video = new Intent(this, VideoPlayerActivity.class).setData(uri);
            startActivity(view_video);
        } else {
            Intent view_photo = new Intent(this, ItemActivity.class)
                    .setData(uri)
                    .putExtra(ItemActivity.ALBUM_ITEM, aI)
                    .putExtra(ItemActivity.VIEW_ONLY, true)
                    .putExtra(ItemActivity.ALBUM, a)
                    .putExtra(ItemActivity.ITEM_POSITION, a.getAlbumItems().indexOf(aI))
                    .addFlags(i.getFlags());
            startActivity(view_photo);
        }
        this.finish();
    }

    private void pick(Intent intent) {
        setIntent(new Intent("ACTIVITY_ALREADY_LAUNCHED"));

        Intent pick_photos = new Intent(this, MainActivity.class)
                .putExtra(Intent.EXTRA_ALLOW_MULTIPLE, intent.getBooleanExtra(Intent.EXTRA_ALLOW_MULTIPLE,
                        false))
                .setAction(MainActivity.PICK_PHOTOS);
        startActivityForResult(pick_photos, MainActivity.PICK_PHOTOS_REQUEST_CODE);
    }

    private void edit(Intent i) {
        String imagePath = i.getStringExtra(EditImageActivity.IMAGE_PATH);

        Intent edit = new Intent(this, EditImageActivity.class)
                .setAction(Intent.ACTION_EDIT)
                .setDataAndType(i.getData(), i.getType())
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
