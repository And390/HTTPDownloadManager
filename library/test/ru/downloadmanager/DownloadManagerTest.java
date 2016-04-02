package ru.downloadmanager;


import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static ru.downloadmanager.DownloadItemState.*;
import static org.junit.Assert.*;

public class DownloadManagerTest {

    private static final File DIR = new File("temp");

    private DownloadManager downloadManager;

    private ConcurrentHashMap<String, ItemController> listenerMap;

    @Before
    public void init() {
        DIR.mkdirs();

        listenerMap = new ConcurrentHashMap<>();
        downloadManager = new DownloadManager(2, new File("temp")) {
            {
                service = new DownloadService() {
                    @Override
                    public void download(String url, File file, int followRedirects, long rangeFrom, Handler handler) throws IOException, DownloadException, InterruptedException {
                        ItemController listener = listenerMap.get(url);
                        if (listener == null) {
                            throw new IllegalArgumentException("Unknow url value: " + url);
                        }
                        listener.start();
                        listener.awaitExecute();
                    }
                };
            }
        };
    }

    private static class ItemController implements CompleteListener {
        String url;  //just name for messages
        int id;
        DownloadItemState state;
        CountDownLatch start;
        CountDownLatch process;
        CountDownLatch complete;

        ItemController(String url) {
            this.url = url;
            this.start = new CountDownLatch(1);
            this.process = new CountDownLatch(1);
            this.complete = new CountDownLatch(1);
        }

        @Override
        public void onComplete(DownloadItemState itemState) {
            state = itemState;
            complete.countDown();
        }

        public void start() {
            start.countDown();
        }

        public void awaitStart() throws InterruptedException {
            if (!start.await(300, TimeUnit.SECONDS)) {
                fail("Haven't waited for start");
            }
        }

        public void execute() {
            process.countDown();
        }

        public void awaitExecute() throws InterruptedException {
            if (!process.await(600, TimeUnit.MILLISECONDS)) {
                fail("Haven't waited for execution");
            }
        }

        public void await() throws InterruptedException {
            if (!complete.await(900, TimeUnit.SECONDS)) {
                fail("Haven't waited for completion");
            }
        }

        public void awaitAndCheckState(DownloadItemState expected) throws InterruptedException {
            await();
            assertEquals("State for: " + url, expected, state);
        }

        public void checkState(DownloadManager downloadManager, DownloadItemState expected) {
            assertEquals("State for: " + url, expected, downloadManager.getState(id).getState());
        }
    }

    private ItemController add(String url, boolean error) throws IOException {
        ItemController item = new ItemController(url);
        if (!error) {
            listenerMap.put(url, item);
        }
        item.id = downloadManager.add(url, item).getId();
        return item;
    }

    private ItemController add(String url) throws IOException {
        return add(url, false);
    }

    @Test
    public void error() throws Exception {
        ItemController item = add("http://one.url", true);

        item.awaitAndCheckState(ERROR);
    }

    @Test
    public void one() throws Exception {
        ItemController item = add("http://one.url");

        item.execute();

        item.awaitAndCheckState(DONE);
    }

    @Test
    public void twoConcurrent() throws Exception {
        downloadManager.setThreadsCount(2);

        ItemController item1 = add("http://first.url");
        ItemController item2 = add("http://second.url");

        item1.execute();
        item2.execute();

        item1.awaitAndCheckState(DONE);
        item2.awaitAndCheckState(DONE);
    }

    @Test
    public void concurrentThirdWait() throws Exception {
        downloadManager.setThreadsCount(2);

        ItemController item1 = add("http://first.url");
        ItemController item2 = add("http://second.url");
        ItemController item3 = add("http://third.url");

        // two item must be started, third item must not
        item1.awaitStart();
        item2.awaitStart();
        item1.checkState(downloadManager, STARTED);
        item2.checkState(downloadManager, STARTED);
        item3.checkState(downloadManager, NEW);

        // after completion of one third element must start
        item1.execute();
        item1.awaitAndCheckState(DONE);
        item3.awaitStart();
        item2.checkState(downloadManager, STARTED);
        item3.checkState(downloadManager, STARTED);

        item2.execute();
        item3.execute();

        item2.awaitAndCheckState(DONE);
        item3.awaitAndCheckState(DONE);
    }

    @Test
    public void serialWithConcurrent() throws Exception {
        ItemController item1 = add("http://first.url");
        ItemController item2 = add("http://second.url");

        // hold first item, other can pass sequentially

        item2.execute();
        item2.awaitAndCheckState(DONE);

        ItemController item3 = add("http://third.url", true);  // with error also OK
        item3.execute();
        item3.awaitAndCheckState(ERROR);

        ItemController item4 = add("http://fourth.url");
        item4.execute();
        item4.awaitAndCheckState(DONE);

        item1.execute();
        item1.awaitAndCheckState(DONE);
    }
}
