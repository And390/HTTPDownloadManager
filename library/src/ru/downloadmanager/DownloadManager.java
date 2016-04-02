package ru.downloadmanager;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


/**
 * End-point class for client.
 * Thread-safe
 */
public class DownloadManager {

    protected DownloadService service;

    private ThreadPoolExecutor pool;
    private File downloadDir;

    private LinkedHashMap<Integer, Item> queue = new LinkedHashMap<>();
    private Object sync = queue;
    private int counter = 0;

    public DownloadManager(int threadsCount, File downloadDir) {
        service = new DownloadService();
        pool = new ThreadPoolExecutor(threadsCount, threadsCount, 1, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());
        this.downloadDir = downloadDir;
    }

    public void setThreadsCount(int count) {
        pool.setCorePoolSize(count);
        pool.setMaximumPoolSize(count);
    }

    public String getUserAgent() {
        return service.getUserAgent();
    }

    public void setUserAgent(String userAgent) {
        service.setUserAgent(userAgent);
    }

    public int getRedirectionLimit() {
        return service.getRedirectionLimit();
    }

    public void setRedirectionLimit(int redirectionLimit) {
        service.setRedirectionLimit(redirectionLimit);
    }


    /**
     * Synchronously add task
     */
    public DownloadItem add(String url, CompleteListener completeListener) throws IOException {
        File file = DownloadService.createFileForURL(url, downloadDir);
        synchronized (sync) {
            Item item = new Item();
            item.id = counter++;
            item.url = url;
            item.file = file;
            item.total = DownloadService.UNKNOWN;
            item.received = 0;
            item.completeListener = completeListener;
            startInternal(item);
            queue.put(item.id, item);
            return convert(item);
        }
    }

    private void startInternal(Item item) {
        item.task = pool.submit(new DownloadTask(item));
    }

    /**
     * Synchronously start task if it was stopped
     * Non-existent and not stopped tasks will be ignored.
     */
    public void start(int id) {
        Item item;
        synchronized (sync) {
            item = queue.get(id);
            if (item != null && (item.state == DownloadItemState.STOPPED || item.state == DownloadItemState.ERROR)) {
                item.state = DownloadItemState.NEW;
                item.error = null;
                //last file will overwrited
                startInternal(item);
            }
        }
    }

    /**
     * Stop task asynchronously or synchronously if possible.
     * Non-existent, already stopped or completed tasks will be ignored,
     */
    public void stop(int id) {
        Item item;
        CompleteListener callStopped = null;
        synchronized (sync) {
            item = queue.get(id);
            if (item == null) {
                return;
            }

            //    don't wait in case of NEW state
            if (item.state == DownloadItemState.NEW) {
                item.state = DownloadItemState.STOPPED;
                item.error = new InterruptedException();
                callStopped = item.completeListener;
            }
        }
        if (callStopped != null) {
            callStopped.onComplete(DownloadItemState.STOPPED);
        }
        item.task.cancel(true);  //can be executed concurrently
    }

    /**
     * Asynchronously stop all tasks. Client needs to listen onStopped event.
     */
    public void stopAll() {
        synchronized (sync) {
            for (Item item : queue.values()) {
                item.task.cancel(true);
            }
        }
    }

    /**
     * Immediately remove task from queue and stop it (as in 'stop' method).
     * Non-existent tasks will be ignored,
     */
    public void remove(int id) {
        Item item;
        synchronized (sync) {
            item = queue.remove(id);
            if (item == null) {
                return;
            }
            item.remove = true;
        }
        item.task.cancel(true);  //can be executed concurrently
    }

    public void shutdown() throws InterruptedException {
        pool.shutdown();
        stopAll();
        while (!pool.awaitTermination(10, TimeUnit.SECONDS)) ;
    }

    public DownloadState getState() {
        synchronized (sync) {
            final int UNKNOWN = DownloadService.UNKNOWN;
            long total = 0;
            long received = 0;
            DownloadItem[] resultItems = new DownloadItem[queue.size()];
            int i = 0;
            for (Item item : queue.values()) {
                resultItems[i++] = convert(item);
                if (item.error == null) {
                    total = total == UNKNOWN ? UNKNOWN :
                            item.state == DownloadItemState.DONE ? total + item.received :
                            item.total == UNKNOWN ? UNKNOWN :
                            item.total < item.received ? total + item.received :
                            total + item.total;
                    received += item.received;
                }
            }
            return new DownloadState(total != UNKNOWN ? total : 0, received, resultItems);
        }
    }

    public DownloadItem getState(int id) {
        synchronized (sync) {
            Item item = queue.get(id);
            return item == null ? null : convert(item);
        }
    }

    private DownloadItem convert(Item item) {
        return new DownloadItem(item.id, item.url, item.file,
                item.total != DownloadService.UNKNOWN && item.total >= item.received ? item.total : 0, item.received,
                item.state, item.error);
    }


    private static class Item {
        int id;
        String url;
        File file;
        long total;
        long received;
        Future<?> task;
        Throwable error;
        DownloadItemState state = DownloadItemState.NEW;
        boolean remove = false;
        CompleteListener completeListener;
    }

    private class DownloadTask implements Runnable {
        private Item item;

        public DownloadTask(Item item) {
            this.item = item;
        }

        @Override
        public void run() {
            Throwable error = null;
            boolean stop = false;
            try {
                synchronized (sync) {
                    if (item.state != DownloadItemState.NEW) {
                        // task was canceled before run or somethind else
                        return;
                    }
                    item.state = DownloadItemState.STARTED;
                }

                service.download(item.url, item.file, 10, (received, total) -> {
                    synchronized (sync) {
                        if (Thread.interrupted()) {
                            throw new InterruptedException();
                        }
                        item.received = received;
                        item.total = total;
                    }
                });
            } catch (InterruptedException e) {
                stop = true;
            } catch (Throwable e) {
                error = e;
            }

            boolean needRemoveFile;
            CompleteListener completeListener;
            DownloadItemState state = stop ? DownloadItemState.STOPPED : error != null ? DownloadItemState.ERROR : DownloadItemState.DONE;
            synchronized (sync) {
                needRemoveFile = item.remove || error != null;
                completeListener = item.completeListener;
                item.error = error;
                item.state = state;
            }
            if (needRemoveFile) {
                item.file.delete();
            }

            if (completeListener != null) {
                completeListener.onComplete(state);
            }
        }
    }
}
