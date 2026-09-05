package com.wmp.downloader.tools.download;

import com.wmp.downloader.newArchitecture.exception.DownloadException;
import com.wmp.downloader.tools.file.DataControl;
import com.wmp.downloader.tools.StringFormat;
import com.wmp.downloader.tools.ui.ToastMessage;
import org.apache.log4j.Logger;

import javax.swing.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class URLDownloadTool {

    private static final Logger logger = Logger.getLogger(URLDownloadTool.class);

    public static boolean isCanUseMultithreading(URI uri, long fileSize){
        return isCanUseMultithreading(uri, fileSize, null);
    }

    public static boolean isCanUseMultithreading(URI uri, long fileSize, Map<String, String> headers){
        try {
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
        } catch (IOException e) {
            logger.error("错误！", e);
            return false;
        }
    }

    public static DownloadingInfo download(URI uri, File destFile, String fileName, long fileSize, int numThreads, int maxRetries, List<JProgressBar> progressBarList, PauseController pauseController, DownloadProgress progress) throws Exception {
        return download(uri, destFile, fileName, fileSize, numThreads, maxRetries, progressBarList, pauseController, progress, null);
    }

    /**
     * 多线程下载
     *
     * @param uri             下载链接
     * @param destFile        保存路径（暂时不用）
     * @param fileName        文件名
     * @param fileSize        文件大小
     * @param numThreads      线程数
     * @param maxRetries      最大重试次数
     * @param progressBarList 进度列表
     * @param pauseController 暂停管理
     * @param progress        进度文字处理
     * @param headers         头部信息
     * @return 下载信息
     */
    public static DownloadingInfo download(URI uri, File destFile, String fileName, long fileSize, int numThreads, int maxRetries, List<JProgressBar> progressBarList, PauseController pauseController, DownloadProgress progress, Map<String, String> headers) throws Exception {

        logger.info("下载的链接：" + uri);

        fileName = StringFormat.sanitizeName(fileName);
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

    /**
     * 单线程下载
     *
     * @param uri             下载链接
     * @param destPath        保存路径
     * @param fileName        文件名
     * @param fileSize        文件大小
     * @param maxRetries      最大重试次数
     * @param progressBar     进度条
     * @param pauseController 暂停管理
     * @param progress        进度文字处理
     * @return 是否下载成功
     */
    public static boolean singleThreadDownload(URI uri, File destPath, String fileName, long fileSize, int maxRetries, JProgressBar progressBar, PauseController pauseController, DownloadProgress progress) throws Exception {
        return singleThreadDownload(uri, destPath, fileName, fileSize, maxRetries, progressBar, pauseController, progress, null);
    }

    /**
     * 单线程下载
     *
     * @param uri             下载链接
     * @param destPath        保存路径
     * @param fileName        文件名
     * @param fileSize        文件大小
     * @param maxRetries      最大重试次数
     * @param progressBar     进度条
     * @param pauseController 暂停管理
     * @param progress        进度文字处理
     * @param headers         头部信息
     * @return 是否下载成功
     */
    public static boolean singleThreadDownload(URI uri, File destPath, String fileName, long fileSize, int maxRetries, JProgressBar progressBar, PauseController pauseController, DownloadProgress progress, Map<String, String> headers) throws Exception {

        logger.info("下载的链接：" + uri);

        File destFile = StringFormat.sanitizeFile(new File(destPath, fileName));
        long downloaded = destFile.exists() && (destFile.length() < fileSize) ? destFile.length() : 0;

        URL url = uri.toURL();
        HttpURLConnection conn;

        if (fileSize <= 0) {
            return fullDownload(uri, destFile, progressBar, pauseController);
        }

        if (destFile.exists()) {
            if (destFile.length() == fileSize) {
                logger.debug("文件已完整下载。");
                progressBar.setValue(100);
                return true;
            } else if (destFile.length() > fileSize) {
                var i = JOptionPane.showConfirmDialog(null, StringFormat.translate("task", "task.download_task.delete_err_file.confirm"));
                if (i == JOptionPane.YES_OPTION) {
                    destFile.delete();
                } else {
                    return true;
                }
            }

        } else {
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
                        if (totalRead >= fileSize) {
                            logger.debug("单线程下载完成。");
                            progressBar.setValue(100);
                            return true;
                        } else {
                            downloaded = totalRead;
                        }
                    }
                } else {

                    if (responseCode == 416) return true;

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

    private static boolean fullDownload(URI uri, File destPath, JProgressBar progressBar, PauseController pauseController) throws Exception {

        logger.info("下载的链接：" + uri);

        if (!destPath.exists()) {
            destPath = StringFormat.sanitizeFile(destPath);
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
                if (pauseController != null) pauseController.checkPause();
                fos.write(buffer, 0, len);
            }
            logger.debug("全量下载完成。");
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 文件合并
     *
     * @param destPath        保存路径
     * @param fileName        文件名
     * @param partCount       分段数
     * @param fileSize        文件大小
     * @param progressBar     进度条
     * @param pauseController 暂停管理
     * @param progress        进度文字处理
     */
    public static void mergeParts(File destPath, String fileName, int partCount, long fileSize, JProgressBar progressBar, PauseController pauseController, DownloadProgress progress) throws IOException {
        logger.info("合并的路径：" + destPath);

        File destFile = StringFormat.sanitizeFile(new File(destPath, fileName));
        if (destFile.exists()) {
            if (!destFile.delete()) {
                ToastMessage.show(StringFormat.translate("delete_failed"));
                throw new DownloadException("文件合并失败");
            }
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
                        if (progress != null) {
                            progress.resetMergedBytes(downloadedSize);
                        }
                        //if (progress != null) progress.addMergedBytes(len);
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

    public static long getFileSize(String urlStr) {
        try {
            // 方式一：HEAD 请求（推荐，不消耗流量）
            HttpURLConnection conn = (HttpURLConnection) URI.create(urlStr).toURL().openConnection();
            conn.setRequestMethod("HEAD");
            conn.connect();
            long totalBytes = conn.getContentLengthLong(); // 注意用 Long，避免 int 溢出（>2GB）

            // 方式二：如果服务器不支持 HEAD，在 GET 请求中加 Range: bytes=0-0
            if (totalBytes <= 0) {
                conn = (HttpURLConnection) URI.create(urlStr).toURL().openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Range", "bytes=0-0");
                conn.connect();
                String contentRange = conn.getHeaderField("Content-Range");
                if (contentRange != null && contentRange.startsWith("bytes 0-0/")) {
                    // 响应头 Content-Range: bytes 0-0/123456789，需解析斜杠后面的值
                    totalBytes = Long.parseLong(contentRange.substring("bytes 0-0/".length()));
                }
            }

            return totalBytes;
        } catch (Exception e) {
            logger.error("获取文件大小失败", e);
            return 0;
        }
    }

    /**
     * 从 URL 和 HttpURLConnection 中提取文件名
     *
     * @param urlStr 下载链接
     * @return 提取的文件名，若无法获取则返回默认值 "downloaded_file"
     */
    public static String extractFileName(String urlStr) throws Exception {
        String fileName = null;

        // 1. 优先从 Content-Disposition 头获取（服务端指定的文件名）
        String disposition = URI.create(urlStr).toURL().openConnection().getHeaderField("Content-Disposition");
        if (disposition != null && !disposition.isEmpty()) {
            fileName = parseContentDisposition(disposition);
        }

        // 2. 若头信息没有，从 URL 路径解析
        if (fileName == null || fileName.isEmpty()) {
            fileName = parseFileNameFromURL(urlStr);
        }

        // 3. 若还是无法获取，返回默认名称
        if (fileName == null || fileName.isEmpty()) {
            fileName = "downloaded_file";
        }
        return fileName;
    }

    private static String parseContentDisposition(String disposition) {
        // 优先匹配 filename*（支持 UTF-8 编码）
        Pattern patternStar = Pattern.compile("filename\\*\\s*=\\s*([^;]+)");
        Matcher matcherStar = patternStar.matcher(disposition);
        if (matcherStar.find()) {
            String value = matcherStar.group(1).trim();
            // 格式: UTF-8''encoded_name
            if (value.startsWith("UTF-8''")) {
                String encoded = value.substring(6);
                try {
                    return URLDecoder.decode(encoded, StandardCharsets.UTF_8);
                } catch (Exception e) {
                    // 解码失败则忽略
                }
            }
        }

        // 匹配普通 filename（带引号或不带引号）
        Pattern pattern = Pattern.compile("filename\\s*=\\s*\"?([^\";]+)\"?");
        Matcher matcher = pattern.matcher(disposition);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }

    private static String parseFileNameFromURL(String urlStr) {
        try {
            URL url = URI.create(urlStr).toURL();
            String path = url.getPath();
            if (path == null || path.isEmpty() || path.endsWith("/")) {
                return null;
            }
            // 取最后一个 '/' 后面的部分
            String fileName = path.substring(path.lastIndexOf('/') + 1);
            // 去掉查询参数（如果有 ? 或 #）
            int queryIdx = fileName.indexOf('?');
            if (queryIdx > 0) {
                fileName = fileName.substring(0, queryIdx);
            }
            int hashIdx = fileName.indexOf('#');
            if (hashIdx > 0) {
                fileName = fileName.substring(0, hashIdx);
            }
            return fileName.isEmpty() ? null : fileName;
        } catch (Exception e) {
            return null;
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
                                    //progressBar.setVisible(false);
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
        private volatile long mergedBytes = 0;
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

        public long getMergedBytes() {
            return mergedBytes;
        }

        public void resetMergedBytes(long bytes) {
            mergedBytes = bytes;
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

    public record DownloadingInfo(CountDownLatch latch, ExecutorService executor,
                                  List<DownloadTaskRunnable> downloadTasks,
                                  int status, PauseController pauseController, DownloadProgress progress) {

    }
}
