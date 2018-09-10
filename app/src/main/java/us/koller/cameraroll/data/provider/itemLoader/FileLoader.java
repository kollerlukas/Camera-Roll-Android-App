package us.koller.cameraroll.data.provider.itemLoader;

import android.content.Context;
import android.os.Environment;

import java.io.File;

import us.koller.cameraroll.data.models.File_POJO;
import us.koller.cameraroll.util.MediaType;

public class FileLoader extends ItemLoader {

    private static File_POJO allFiles;

    private File_POJO dir_pojo;

    public FileLoader() {
        if (allFiles == null) {
            allFiles = new File_POJO(Environment.getExternalStorageDirectory().getPath(), false);
        }
    }

    private static void addFiles(File_POJO fs, File_POJO fTA) {
        if (fs.getPath().equals(fTA.getPath())) {
            fs.getChildren().addAll(fTA.getChildren());
        } else if (fs.getPath().equals(fTA.getPath()
                .replace("/" + fTA.getName(), ""))) {
            fs.addChild(fTA);
        } else {
            File_POJO cF = fs;

            String[] fTAP = fTA.getPath().split("/");
            for (int i = 0; i < fTAP.length; i++) {
                boolean found = false;
                for (int k = 0; k < cF.getChildren().size(); k++) {
                    if (fTAP[i].equals(cF.getChildren().get(k).getName())) {
                        found = true;
                        cF = cF.getChildren().get(k);
                    }
                }
                if (found) {
                    cF.addChild(fTA);
                    break;
                }
            }
        }
    }

    @Override
    public ItemLoader newInstance() {
        return new FileLoader();
    }

    @Override
    public void onNewDir(Context c, File dir) {
        dir_pojo = new File_POJO(dir.getPath(), MediaType.isMedia(dir.getPath()));
    }

    @Override
    public void onFile(Context c, File f) {
        File_POJO file_pojo = new File_POJO(f.getPath(),
                MediaType.isMedia(f.getPath()));
        dir_pojo.addChild(file_pojo);
    }

    @Override
    public void onDirDone(Context c) {
        addFiles(allFiles, dir_pojo);
    }

    @Override
    public Result getResult() {
        Result r = new Result();
        r.files = dir_pojo;
        return r;
    }
}
