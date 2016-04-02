package ru.downloadmanager;


import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import sun.misc.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class DownloadServiceTest {

    private static final String DIR_PATH = "temp/";

    private Random random = new Random();

    private DownloadService service;
    private byte[] source;
    private int maxReadBlock;

    private Map<String, String> redirectMap;
    private List<String> accessedURLs;

    private DownloadService.Handler downloadHandler;
    private long received;
    private long total;

    @Before
    public void init() {
        File dir = new File(DIR_PATH);
        if (!dir.mkdirs()) {
            for (File file : dir.listFiles()) {
                file.delete();
            }
        }

        source = generateRandomBytes(16384);
        maxReadBlock = Integer.MAX_VALUE;
        redirectMap = new HashMap<>();
        accessedURLs = new ArrayList<>();
        service = new DownloadService() {
            @Override
            protected HttpURLConnection openConnection(String requestUrl) throws IOException {
                accessedURLs.add(requestUrl);
                return new HttpURLConnection(new URL(requestUrl)) {
                    int sourcePosition = 0;
                    int range = 0;

                    @Override
                    public void disconnect() {
                    }

                    @Override
                    public boolean usingProxy() {
                        return false;
                    }

                    @Override
                    public void connect() throws IOException {
                    }

                    @Override
                    public int getResponseCode() throws IOException {
                        if (redirectMap.containsKey(requestUrl)) {
                            return HttpURLConnection.HTTP_MOVED_TEMP;
                        }
                        if (range != 0) {
                            return HttpURLConnection.HTTP_PARTIAL;
                        }
                        return HttpURLConnection.HTTP_OK;
                    }

                    @Override
                    public String getHeaderField(String name) {
                        if (name == null) {
                            return null;
                        }
                        if (name.equals("Location")) {
                            return redirectMap.get(requestUrl);
                        }
                        if (name.equals("Content-Range")) {
                            return "bytes " + range + "-" + (source.length-range-1) + "/" + source.length;
                        }
                        return null;
                    }

                    @Override
                    public void setRequestProperty(String key, String value) {
                        if (key.equals("Range")) {
                            Matcher matcher = Pattern.compile("bytes=([0-9]+)-").matcher(value);
                            if (!matcher.matches()) {
                                throw new RuntimeException("Unsupported 'Range' format: " + value);
                            }
                            sourcePosition = range = Integer.valueOf(matcher.group(1));
                        }
                    }

                    @Override
                    public int getContentLength() {
                        return source.length - range;
                    }

                    @Override
                    public InputStream getInputStream() throws IOException {
                        return new InputStream() {
                            @Override
                            public int read() throws IOException {
                                if (sourcePosition == source.length) {
                                    return -1;
                                }
                                return source[sourcePosition++];
                            }

                            @Override
                            public int read(byte[] b, int off, int len) throws IOException {
                                if (source.length == sourcePosition) {
                                    return -1;
                                }
                                len = Math.min(len, maxReadBlock);
                                len = Math.min(len, source.length - sourcePosition);
                                System.arraycopy(source, sourcePosition, b, off, len);
                                sourcePosition += len;
                                return len;
                            }
                        };
                    }
                };
            }
        };

        received = 0;
        total = DownloadService.UNKNOWN;
        downloadHandler = (long received, long total) -> {
            DownloadServiceTest.this.received = received;
            DownloadServiceTest.this.total = total;
        };
    }

    @AfterClass
    public static void finz() {
        File dir = new File(DIR_PATH);
        if (dir.exists()) {
            for (File file : dir.listFiles()) {
                file.delete();
            }
            dir.delete();
        }
    }

    private byte[] generateRandomBytes(int size) {
        byte[] buffer = new byte[size];
        for (int i=0; i<size; i++) {
            if (random.nextInt(7) == 0) {
                buffer[i] = ' ';
            } else {
                buffer[i] = (byte) (random.nextInt('z' - 'a') + 'a');
            }
        }
        return buffer;
    }

    @Test
    public void getNewFile() {
        File file = DownloadService.getFileForURL("www.domen.com/picture.jpg", DIR_PATH);
        assertEquals("picture.jpg", file.getName());
    }

    @Test
    public void getFileIfExists() throws IOException {
        new File(DIR_PATH + "/picture.jpg").createNewFile();
        File file = DownloadService.getFileForURL("www.domen.com/picture.jpg", DIR_PATH);
        assertEquals("picture (2).jpg", file.getName());
    }

    @Test
    public void getFileIfSecondExists() throws IOException {
        new File(DIR_PATH + "/picture.jpg").createNewFile();
        new File(DIR_PATH + "/picture (2).jpg").createNewFile();
        File file = DownloadService.getFileForURL("www.domen.com/picture.jpg", DIR_PATH);
        assertEquals("picture (3).jpg", file.getName());
    }

    @Test
    public void download() throws Exception {
        File file = new File(DIR_PATH + "/temp");

        service.download("http://any.url", file, downloadHandler);

        assertEquals(Collections.singletonList("http://any.url"), accessedURLs);
        try (FileInputStream in = new FileInputStream(file)) {
            assertEquals("received bytes count", source.length, received);
            assertEquals("received Content-Length", source.length, total);
            byte[] result = IOUtils.readFully(in, -1, false);
            assertArrayEquals("received bytes", source, result);
        }
    }

    @Test
    public void downloadBySmellParts() throws Exception {
        File file = new File(DIR_PATH + "/temp");

        maxReadBlock = 23;
        service.download("http://any.url", file, downloadHandler);

        try (FileInputStream in = new FileInputStream(file)) {
            byte[] result = IOUtils.readFully(in, -1, false);
            assertArrayEquals("received bytes", source, result);
        }
    }

    @Test
    public void redirect() throws Exception {
        File file = new File(DIR_PATH + "/temp");

        redirectMap.put("http://first", "http://second");
        redirectMap.put("http://second", "http://third");
        service.setFollowRedirects(2);
        service.download("http://first", file, downloadHandler);

        assertEquals(Arrays.asList("http://first", "http://second", "http://third"), accessedURLs);
        try (FileInputStream in = new FileInputStream(file)) {
            byte[] result = IOUtils.readFully(in, -1, false);
            assertArrayEquals("received bytes", source, result);
        }
    }

    @Test(expected = DownloadException.class)
    public void toManyRedirects() throws Exception {
        File file = new File(DIR_PATH + "/temp");

        redirectMap.put("http://first", "http://second");
        redirectMap.put("http://second", "http://third");
        redirectMap.put("http://third", "http://fourth");
        service.setFollowRedirects(2);
        service.download("http://first", file, downloadHandler);
    }

    @Test
    public void downloadRange() throws Exception {
        File file = new File(DIR_PATH + "/temp");

        int range = 997;
        service.download("http://any.url", file, range, downloadHandler);

        try (FileInputStream in = new FileInputStream(file)) {
            assertEquals("received bytes count", source.length, received);
            assertEquals("received Content-Length", source.length, total);
            byte[] result = IOUtils.readFully(in, -1, false);
            byte[] expected = new byte[source.length - range];
            System.arraycopy(source, range, expected, 0, expected.length);
            assertArrayEquals("received bytes", expected, result);
        }
    }
}
