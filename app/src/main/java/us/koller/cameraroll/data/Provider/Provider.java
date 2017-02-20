package us.koller.cameraroll.data.Provider;

import us.koller.cameraroll.data.Provider.Retriever.Retriever;

abstract class Provider {

    Retriever retriever;

    Provider() {

    }

    public void onDestroy() {
        if (retriever != null) {
            retriever.onDestroy();
        }
    }
}
