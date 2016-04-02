package ru.downloadmanager;

import org.junit.Test;

import java.io.File;


public class DownloadSingleURL {

    @Test
    public void print() throws Exception {
        String url = "http://www.wallpaperhdf.com/wp-content/uploads/2016/02/beautiful-landscape-wallpapers-photos-background.jpg";
        File file = new File("temp");

        DownloadService service = new DownloadService();
        service.download(url, file, 0, new DownloadService.Handler() {
            @Override
            public void onProgress(long received, long total) throws InterruptedException {
                System.out.println(received + " / " + total);
            }
        });
    }

}
