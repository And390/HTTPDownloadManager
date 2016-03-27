package ru.downloadmanager;


/**
 * Constant snapshot of download manager state
 */
public class DownloadState {

    private long total;  //zero if unknown
    private long received;
    private DownloadItem[] items;

    public DownloadState(long total, long received, DownloadItem[] items) {
        this.total = total;
        this.received = received;
        this.items = items;
    }

    public long getTotal() {
        return total;
    }

    public long getReceived() {
        return received;
    }

    public DownloadItem[] getItems() {
        return items;
    }
}
