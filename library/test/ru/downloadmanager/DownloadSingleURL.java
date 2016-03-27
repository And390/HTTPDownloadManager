package ru.downloadmanager;

import org.junit.Test;
import sun.misc.IOUtils;

import java.io.File;
import java.io.FileInputStream;


public class DownloadSingleURL {

    @Test
    public void print() throws Exception {
        String url = "http://www.wallpaperhdf.com/wp-content/uploads/2016/02/beautiful-landscape-wallpapers-photos-background.jpg";
        File file = new File("temp");

        DownloadService.download(url, file, 0, new DownloadService.Handler() {
            @Override
            public void onProgress(long received, long total) throws InterruptedException {
                System.out.println(received + " / " + total);
            }
        });
    }

}
