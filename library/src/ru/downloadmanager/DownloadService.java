package ru.downloadmanager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;


public class DownloadService {

    public static int UNKNOWN = -1;

    private String userAgent = "Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:43.0) Gecko/20100101 Firefox/43.0";
    private int redirectionLimit = 5;

    public DownloadService() {

    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public int getRedirectionLimit() {
        return redirectionLimit;
    }

    public void setRedirectionLimit(int redirectionLimit) {
        this.redirectionLimit = redirectionLimit;
    }

    public interface Handler {
        void onProgress(long received, long total) throws InterruptedException;
    }

    public void download(String url, File file, Handler handler) throws IOException, DownloadException, InterruptedException {
        download(url, file, file.exists() ? (int)file.length() : 0, handler);
    }

    public void download(String url, File file, long rangeFrom, Handler handler) throws IOException, DownloadException, InterruptedException {
        download(url, file, redirectionLimit, rangeFrom, handler);
    }

    public void download(String url, File file, int followRedirects, long rangeFrom, Handler handler) throws IOException, DownloadException, InterruptedException {

        //    prepare request
        HttpURLConnection connection = openConnection(url);
        connection.setRequestMethod("GET");
        connection.setRequestProperty("User-Agent", userAgent);
        if (rangeFrom > 0) {
            connection.setRequestProperty("Range", "bytes=" + rangeFrom + "-");
        }

        //    check ranged response
        int status = connection.getResponseCode();
        boolean rangedResponse = rangeFrom > 0 && status == HttpURLConnection.HTTP_PARTIAL;
        if (rangedResponse) {
            String contentRange = connection.getHeaderField("Content-Range");
            if (contentRange == null) {
                throw new DownloadException("Invalid HTTP response, no 'Content-Range' header for status code " + status);
            }
            if (!contentRange.matches("bytes " + rangeFrom + "-[0-9]+/[0-9]+")) {
                throw new DownloadException("Invalid HTTP response, wrong 'Content-Range' header value");
            }

        //    process redirects or error status code
        } else if (status != HttpURLConnection.HTTP_OK) {
            if (followRedirects >= 0 && (status == HttpURLConnection.HTTP_MOVED_TEMP || status == HttpURLConnection.HTTP_MOVED_PERM
                    || status == HttpURLConnection.HTTP_SEE_OTHER || status == 307)) {
                if (followRedirects == 0) {
                    throw new DownloadException("No more redirects are allowed");
                }
                String location = connection.getHeaderField("Location");
                if (location == null) {
                    throw new DownloadException("Invalid HTTP response, no 'Location' header for status code " + status);
                }
                download(location, file, followRedirects - 1, rangeFrom, handler);
                return;
            } else {
                throw new DownloadException("Unsuccess status code: " + status);
            }
        }

        //    load content
        long received = rangedResponse ? rangeFrom : 0;
        long total = received + connection.getContentLength();
        handler.onProgress(received, total);

        InputStream in = connection.getInputStream();
        try (FileOutputStream out = new FileOutputStream(file, rangedResponse)) {
            final int BLOCK_SIZE = 8192;
            byte[] buffer = new byte[BLOCK_SIZE + 4096];
            for (int offset = 0; ; ) {
                int readed = in.read(buffer, offset, buffer.length - offset);
                if (readed == -1) {
                    out.write(buffer, 0, offset);
                    break;
                }
                offset += readed;
                if (offset >= BLOCK_SIZE) {
                    out.write(buffer, 0, offset);
                    offset = 0;
                }
                received += readed;
                handler.onProgress(received, total);
            }
        } catch (IOException | RuntimeException e) {
            file.delete();
            throw e;
        }
    }

    protected HttpURLConnection openConnection(String url) throws IOException {
        return (HttpURLConnection) new URL(url).openConnection();
    }

    public static File createFileForURL(String url, File downloadDir) throws IOException {
        while (true) {
            File file = getFileForURL(url, downloadDir);
            if (file.createNewFile()) {
                return file;
            }
        }
    }

    public static File getFileForURL(String url, File downloadDir) {
        if (url.startsWith("http://")) {
            url = url.substring("http://".length());
        } else if (url.startsWith("https://")) {
            url = url.substring("https://".length());
        }
        int i = url.indexOf('?');
        if (i != -1) {
            url = url.substring(0, i);
        }
        i = url.lastIndexOf('/');
        if (i != -1 && i != url.length() - 1) {
            url = url.substring(i+1);
        }
        String fileName = url.replaceAll("[^a-zA-Z0-9\\.]+$", "").replaceAll("[^a-zA-Z0-9\\.]", "_");
        File file = new File(incFileNameWhileExists(new File(downloadDir, fileName).getPath()));
        return file;
    }

    // Add (2) to file name before extension, or inrease the this number if it exists
    private static String incFileName(String fileName, boolean lastExtension) {
        //    get file name start position (last slash or other separator)
        int i0 = fileName.lastIndexOf('/') + 1;
        if (!File.separator.equals("/")) {
            int i02 = fileName.lastIndexOf(File.separator) + 1;
            if (i02 > i0) i0 = i02;
        }
        //    get file extension position (first dot in name, or last if such option is setted)
        int i;
        if (lastExtension) {
            i = fileName.lastIndexOf('.');
            if (i < i0) i = fileName.length();
        } else {
            i = fileName.indexOf('.', i0);
            if (i == -1) {
                i = fileName.length();
            }
        }
        //    find number inside brackets
        if (i >= 4 && fileName.charAt(i - 1) == ')') {
            int i2 = fileName.lastIndexOf('(', i - 2);
            if (i2 > i0)
                try {
                    int count = Integer.parseInt(fileName.substring(i2 + 1, i - 1));
                    return fileName.substring(0, i2 + 1) + (count + 1) + fileName.substring(i - 1);
                } catch (NumberFormatException e) {
                }
        }
        //    return (2) if number is not found
        return fileName.substring(0, i) + " (2)" + fileName.substring(i);
    }

    private static String incFileName(String fileName) {
        return incFileName(fileName, false);
    }

    // Do the same cyclically while file under such name exists
    private static String incFileNameWhileExists(String fileName) {
        while (new File(fileName).exists()) fileName = incFileName(fileName);
        return fileName;
    }
}
