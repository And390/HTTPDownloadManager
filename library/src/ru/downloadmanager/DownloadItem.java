package ru.downloadmanager;

import java.io.File;

/**
* Constant snapshot of downloading item state
*/
public class DownloadItem {
    private final int id;
    private final String url;
    private final File file;
    private final long total;  //zero if unknown
    private final long received;
    private final boolean done;
    private final boolean stopped;
    private final Throwable error;

    public DownloadItem(int id, String url, File file, long total, long received, boolean done, boolean stopped, Throwable error) {
        this.id = id;
        this.url = url;
        this.file = file;
        this.total = total;
        this.received = received;
        this.done = done;
        this.stopped = stopped;
        this.error = error;
    }

    public int getId() {
        return id;
    }

    public String getUrl() {
        return url;
    }

    public File getFile() {
        return file;
    }

    public long getTotal() {
        return total;
    }

    public long getReceived() {
        return received;
    }

    public Throwable getError() {
        return error;
    }

    public boolean isDone() {
        return done;
    }

    public boolean isStopped() {
        return stopped;
    }
}
