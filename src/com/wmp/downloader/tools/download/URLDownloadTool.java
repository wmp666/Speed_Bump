package com.wmp.downloader.tools.download;

import com.wmp.downloader.tools.DataControl;
import org.apache.log4j.Logger;

import javax.swing.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

public class URLDownloadTool {

    private static final Logger logger = Logger.getLogger(URLDownloadTool.class);

    public static boolean isCanUseMultithreading(URI uri, long fileSize) throws Exception {
        return isCanUseMultithreading(uri, fileSize, null);
    }

    public static boolean isCanUseMultithreading(URI uri, long fileSize, Map<String, String> headers) throws Exception {
        URL url = uri.toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("HEAD");
        if (headers != null) {
            for (var entry : headers.entrySet()) {
                conn.setRequestProperty(entry.getKey(), entry.getValue());
            }
        }
        conn.connect();

        boolean acceptRanges = "bytes".equalsIgnoreCase(conn.getHeaderField("Accept-Ranges"));
        conn.disconnect();

        if (fileSize <= 0) {
            logger.warn("无法获取文件大小，使用单线程下载...");
            return false;
        }

        if (!acceptRanges) {
            logger.warn("服务器不支持 Range，使用单线程下载...");
            return false;
        }

        return true;
    }

    public static DownloadingInfo download(URI uri, File destFile, String fileName, long fileSize, int numThreads, int maxRetries, List<JProgressBar> progressBarList, PauseController pauseController, DownloadProgress progress) throws Exception {
        return download(uri, destFile, fileName, fileSize, numThreads, maxRetries, progressBarList, pauseController, progress, null);
    }

    public static DownloadingInfo download(URI uri, File destFile, String fileName, long fileSize, int numThreads, int maxRetries, List<JProgressBar> progressBarList, PauseController pauseController, DownloadProgress progress, Map<String, String> headers) throws Exception {

        File partsFile = new File(DataControl.getTempPath(), fileName);
        var files = partsFile.listFiles(File::isFile);
        if (files != null && files.length != numThreads) {
            deletePartFiles(fileName, new JProgressBar(0, 100));
        }

        // 分段下载
        long start = 0;
        long end;
        long partSize = fileSize / numThreads;
        List<DownloadTaskRunnable> tasks = new ArrayList<>();
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        CountDownLatch latch = new CountDownLatch(numThreads);

        for (int i = 0; i < numThreads; i++) {
            start = i * partSize;
            end = (i == numThreads - 1) ? fileSize - 1 : (start + partSize - 1);
            if (start > fileSize) break;
            File partFile = new File(partsFile, i + ".part");
            partFile.getParentFile().mkdirs();
            partFile.createNewFile();
            tasks.add(new DownloadTaskRunnable(uri.toURL(), partFile, start, end, maxRetries, latch, progressBarList.get(i), pauseController, progress, headers));
        }

        for (DownloadTaskRunnable task : tasks) {
            executor.submit(task);
        }

        return new DownloadingInfo(latch, executor, tasks, 2, pauseController, progress);
    }

    public static boolean singleThreadDownload(URI uri, File destPath, String fileName, long fileSize, int maxRetries, JProgressBar progressBar, PauseController pauseController, DownloadProgress progress) throws Exception {
        return singleThreadDownload(uri, destPath, fileName, fileSize, maxRetries, progressBar, pauseController, progress, null);
    }

    public static boolean singleThreadDownload(URI uri, File destPath, String fileName, long fileSize, int maxRetries, JProgressBar progressBar, PauseController pauseController, DownloadProgress progress, Map<String, String> headers) throws Exception {
        File destFile = new File(destPath, fileName);
        long downloaded = destFile.exists() ? destFile.length() : 0;

        URL url = uri.toURL();
        HttpURLConnection conn;

        if (fileSize <= 0) {
            return fullDownload(uri, destFile, progressBar);
        }

        if (destFile.exists() && destFile.length() == fileSize) {
            logger.debug("文件已完整下载。");
            progressBar.setValue(100);
            return true;
        }else{
            destFile.getParentFile().mkdirs();
            destFile.createNewFile();
        }

        int retries = 0;
        while (retries <= maxRetries) {
            try {
                conn = (HttpURLConnection) url.openConnection();
                if (headers != null) {
                    for (var entry : headers.entrySet()) {
                        conn.setRequestProperty(entry.getKey(), entry.getValue());
                    }
                }
                if (downloaded > 0 && downloaded < fileSize) {
                    conn.setRequestProperty("Range", "bytes=" + downloaded + "-");
                }
                conn.connect();
                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_PARTIAL || responseCode == HttpURLConnection.HTTP_OK) {
                    try (InputStream in = conn.getInputStream();
                         RandomAccessFile raf = new RandomAccessFile(destFile, "rw")) {
                        raf.seek(downloaded);
                        byte[] buffer = new byte[8192];
                        int len;
                        long totalRead = downloaded;
                        while ((len = in.read(buffer)) != -1) {
                            raf.write(buffer, 0, len);
                            totalRead += len;
                            if (pauseController != null) pauseController.checkPause();
                            if (progress != null) progress.addDownloadedBytes(len);
                            long finalTotalRead = totalRead;
                            SwingUtilities.invokeLater(() -> progressBar.setValue((int) ((double) finalTotalRead / fileSize * 100)));
                        }
                        if (totalRead == fileSize) {
                            System.out.println("单线程下载完成。");
                            return true;
                        } else {
                            downloaded = totalRead;
                        }
                    }
                } else {
                    throw new IOException("服务器返回非预期状态码: " + responseCode);
                }
            } catch (Exception e) {
                logger.error("单线程下载失败，重试 " + retries + "/" + maxRetries, e);
                retries++;
                if (retries > maxRetries) {
                    throw new Exception("单线程下载最终失败", e);
                }
                Thread.sleep(2000);
            }
        }
        return true;
    }

    private static boolean fullDownload(URI uri, File destPath, JProgressBar progressBar) throws Exception {

        if (!destPath.exists()){
            destPath.getParentFile().mkdirs();
            destPath.createNewFile();
        }

        URL url = uri.toURL();
        progressBar.setIndeterminate(true);
        try (InputStream in = url.openStream();
             FileOutputStream fos = new FileOutputStream(destPath)) {
            byte[] buffer = new byte[8192];
            int len;
            while ((len = in.read(buffer)) != -1) {
                fos.write(buffer, 0, len);
            }
            logger.debug("全量下载完成。");
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static void mergeParts(File destPath, String fileName, int partCount, long fileSize, JProgressBar progressBar, PauseController pauseController, DownloadProgress progress) throws IOException {
        File destFile = new File(destPath, fileName);
        if (destFile.exists()) {
            destFile.delete();
        } else {

            destPath.mkdirs();
            destFile.createNewFile();
        }

        if (fileSize <= 0) {
            progressBar.setIndeterminate(true);
        }
        try (FileOutputStream fos = new FileOutputStream(destFile, true)) {
            int downloadedSize = 0;
            for (int i = 0; i < partCount; i++) {
                File partFile = new File(DataControl.getTempPath(), fileName + "/" + i + ".part");
                if (!partFile.exists()) {
                    throw new IOException("分段文件缺失: " + partFile);
                }
                try (FileInputStream fis = new FileInputStream(partFile)) {
                    byte[] buffer = new byte[8192];
                    int len;
                    while ((len = fis.read(buffer)) != -1) {
                        if (pauseController != null) pauseController.checkPause();
                        downloadedSize += len;
                        fos.write(buffer, 0, len);
                        if (progress != null) progress.addMergedBytes(len);
                        int finalDownloadedSize = downloadedSize;
                        SwingUtilities.invokeLater(() -> {
                            if (fileSize > 0) {
                                progressBar.setValue((int) ((double) finalDownloadedSize / fileSize * 100));
                            }
                        });
                    }
                }
                partFile.delete();
            }
        }
        logger.debug("合并完成: " + destPath);
    }

    public static void deletePartFiles(String fileName, JProgressBar progressBar) {
        File partFile = new File(DataControl.getTempPath(), fileName);
        var files = partFile.listFiles();
        for (int i = 0; i < files.length; i++) {
            progressBar.setValue((int) ((double) i / partFile.listFiles().length * 100));
            files[i].delete();
        }

    }

    public static class DownloadTaskRunnable implements Runnable {
        private final URL url;
        private final File partFile;
        private final long start;
        private final long end;
        private final int maxRetries;
        private final JProgressBar progressBar;
        private final CountDownLatch latch;
        private final PauseController pauseController;
        private final DownloadProgress progress;
        private final Map<String, String> headers;
        private volatile boolean error = false;

        public DownloadTaskRunnable(URL url, File partFile, long start, long end, int maxRetries, CountDownLatch latch, JProgressBar progressBar, PauseController pauseController, DownloadProgress progress) {
            this(url, partFile, start, end, maxRetries, latch, progressBar, pauseController, progress, null);
        }

        public DownloadTaskRunnable(URL url, File partFile, long start, long end, int maxRetries, CountDownLatch latch, JProgressBar progressBar, PauseController pauseController, DownloadProgress progress, Map<String, String> headers) {
            this.url = url;
            this.partFile = partFile;
            this.start = start;
            this.end = end;
            this.maxRetries = maxRetries;
            this.latch = latch;
            this.progressBar = progressBar;
            this.pauseController = pauseController;
            this.progress = progress;
            this.headers = headers;
        }

        public boolean hasError() {
            return error;
        }

        @Override
        public void run() {
            try {
                File part = partFile;


                long downloaded = part.exists() ? part.length() : 0;
                long rangeSize = end - start + 1;

                if (downloaded == rangeSize) {
                    logger.debug(Thread.currentThread().getName() + " 段已下载完成，跳过。");
                    progressBar.setValue(100);
                    return;
                }

                for (int retry = 0; retry <= maxRetries; retry++) {
                    try {
                        long startPos = start + downloaded;
                        long finalDownloaded = downloaded;
                        SwingUtilities.invokeLater(() -> progressBar.setValue((int) ((double) finalDownloaded / rangeSize * 100)));
                        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                        if (headers != null) {
                            for (var entry : headers.entrySet()) {
                                conn.setRequestProperty(entry.getKey(), entry.getValue());
                            }
                        }
                        conn.setRequestProperty("Range", "bytes=" + startPos + "-" + end);
                        conn.connect();
                        int responseCode = conn.getResponseCode();
                        if (responseCode == HttpURLConnection.HTTP_PARTIAL || responseCode == HttpURLConnection.HTTP_OK) {
                            try (InputStream in = conn.getInputStream();
                                 RandomAccessFile raf = new RandomAccessFile(part, "rw")) {
                                raf.seek(downloaded);
                                byte[] buffer = new byte[8192];
                                int len;
                                long totalRead = downloaded;
                                while ((len = in.read(buffer)) != -1) {
                                    raf.write(buffer, 0, len);
                                    totalRead += len;
                                    if (pauseController != null) pauseController.checkPause();
                                    downloaded = totalRead;
                                    if (progress != null) progress.addDownloadedBytes(len);
                                    long finalDownloaded1 = downloaded;
                                    SwingUtilities.invokeLater(() -> progressBar.setValue((int) ((double) finalDownloaded1 / rangeSize * 100)));
                                }
                                if (totalRead == rangeSize) {
                                    logger.debug(Thread.currentThread().getName() + " 段下载完成。");
                                    progressBar.setVisible(false);
                                    return;
                                } else {
                                    throw new IOException("下载数据不完整，期望 " + rangeSize + "，实际 " + totalRead);
                                }
                            }
                        } else {
                            throw new IOException("响应码: " + responseCode);
                        }
                    } catch (Exception e) {
                        logger.error(Thread.currentThread().getName() + " 段下载失败，重试 " + retry + "/" + maxRetries, e);
                        if (retry == maxRetries) {
                            error = true;
                            throw new Exception("达到最大重试次数，放弃该段", e);
                        }
                        Thread.sleep(2000L * (retry + 1));
                    }
                }
            } catch (Exception e) {
                error = true;
                logger.error("DownloadTaskRunnable error", e);
            } finally {
                latch.countDown();
            }
        }
    }

    public static class PauseController {
        private volatile boolean paused = false;

        public void pause() {
            paused = true;
        }

        public void resume() {
            paused = false;
        }

        public boolean isPaused() {
            return paused;
        }

        public void checkPause() {
            while (paused) {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }

    public static class DownloadProgress {
        private final AtomicLong downloadedBytes = new AtomicLong(0);
        private final AtomicLong mergedBytes = new AtomicLong(0);
        private long lastSampleBytes = 0;
        private long lastSampleTime = System.nanoTime();
        private volatile long speed = 0;

        public static String formatSize(long bytes) {
            if (bytes < 1024) return bytes + " B";
            if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
            if (bytes < 1024L * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
            return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
        }

        public void addDownloadedBytes(long delta) {
            downloadedBytes.addAndGet(delta);
        }

        public long getDownloadedBytes() {
            return downloadedBytes.get();
        }

        public void addMergedBytes(long delta) {
            mergedBytes.addAndGet(delta);
        }

        public long getMergedBytes() {
            return mergedBytes.get();
        }

        public void resetMergedBytes() {
            mergedBytes.set(0);
        }

        public void updateSpeed() {
            long now = System.nanoTime();
            long currentBytes = downloadedBytes.get();
            long elapsed = now - lastSampleTime;
            if (elapsed > 0) {
                speed = (currentBytes - lastSampleBytes) * 1_000_000_000L / elapsed;
                if (speed < 0) speed = 0;
            }
            lastSampleBytes = currentBytes;
            lastSampleTime = now;
        }

        public long getSpeed() {
            return speed;
        }

        public void resetSpeed() {
            lastSampleBytes = downloadedBytes.get();
            lastSampleTime = System.nanoTime();
            speed = 0;
        }
    }

    public record DownloadingInfo(CountDownLatch latch, ExecutorService executor, List<DownloadTaskRunnable> downloadTasks,
                                  int status, PauseController pauseController, DownloadProgress progress) {

    }
}
