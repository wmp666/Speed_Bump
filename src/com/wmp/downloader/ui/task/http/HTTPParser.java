package com.wmp.downloader.ui.task.http;

import com.wmp.downloader.ui.task.Parser;
import com.wmp.downloader.ui.task.createTask.LinkFileInfoPanel;
import org.apache.log4j.Logger;

import java.awt.event.ActionEvent;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HTTPParser extends Parser {

    private static final Logger logger = Logger.getLogger(HTTPParser.class);
    @Override
    public LinkFileInfoPanel parse(String link) {
        try {
            var fileName = extractFileName(link);
            var fileSizeNum = getFileSize(link);
            return LinkFileInfoPanel
                    .createBasicLinkFileInfoPanel(fileName, fileSizeNum, "HTTP", link);
        } catch (Exception e) {
            logger.error("Error parsing HTTP link: "+link, e);
        }
        return LinkFileInfoPanel
                .createBasicLinkFileInfoPanel("", 0, "HTTP", link);
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

    private static long getFileSize(String urlStr) throws Exception {
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
    }
}
