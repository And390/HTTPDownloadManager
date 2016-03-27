package ru.downloadmanager;


public interface CompleteListener {

    /**
     * Called when downloading completed by any reason
     */
    void onComplete(DownloadItemState itemState);

}
