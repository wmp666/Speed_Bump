package com.wmp.downloader.tools.download;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import org.apache.log4j.Logger;
import org.jsoup.Connection;
import org.jsoup.Jsoup;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GopeedClient {

    private static final Logger logger = Logger.getLogger(GopeedClient.class);

    private final String baseUrl;

    /**
     * 构造客户端
     *
     * @param host GopeedClient 服务地址，如 "127.0.0.1"
     * @param port GopeedClient API 端口，如 9999
     */
    public GopeedClient(String host, int port) {
        this.baseUrl = "http://" + host + ":" + port + "/api/v1";
    }

    public static void main(String[] args) throws Exception {
        // 1. 创建客户端（替换为实际地址、端口和令牌）
        GopeedClient client = new GopeedClient("127.0.0.1", 9999);

        // 2. 添加下载任务
        String link =
                "E:\\Users\\21348\\Downloads\\[DL] Escape the Backrooms [P] [RUS + ENG + 7 ENG] (2025, Horror) (1.2510) [Portable] [rutracker-6247724].torrent";
        //"magnet:?xt=urn:btih:509135ce29acc152de151c1e37e09930f16f23b5&dn=zh-cn_windows_11_consumer_editions_version_25h2_updated_june_2026_x64_dvd_2045a41c.iso&xl=8762877952";

        //"ed2k://|file|zh-cn_windows_11_consumer_editions_version_26h1_updated_july_2026_x64_dvd_f69a9a1e.iso|8309725184|8410831F58154C4D3222BD462A9E76B6|/";
        String taskId = client.createTask(link);
        System.out.println("任务创建成功，ID: " + taskId);

        // 3. 轮询查看进度
        for (int i = 0; true; i++) {
            Thread.sleep(3000);
            List<TaskInfo> tasks = client.listTasks();
            for (TaskInfo t : tasks) {
                System.out.println(t);
            }
        }
    }

    /**
     * 发送带认证的 GET 请求
     */
    private String get(String path) throws IOException {
        Connection.Response response = Jsoup.connect(baseUrl + path)
                .ignoreContentType(true)  // 关键：让 Jsoup 不尝试解析 HTML
                .timeout(30000)
                .execute();
        return response.body();
    }

    // ========== 核心业务方法 ==========

    /**
     * 发送带认证的 POST 请求（JSON 请求体）
     */
    private String post(String path, String jsonBody) throws IOException {
        Connection.Response response = Jsoup.connect(baseUrl + path)
                .header("Content-Type", "application/json")
                .requestBody(jsonBody)
                .ignoreContentType(true)
                .timeout(30000)
                .method(Connection.Method.POST)
                .execute();
        return response.body();
    }

    /**
     * 创建下载任务
     */
    public String createTask(String ed2kLink) throws IOException {
        String json = JSONObject.of("req", JSONObject.of("url", ed2kLink)).toString();
        String resp = post("/tasks", json);
        JSONObject obj = JSONObject.parseObject(resp);
        logger.info(obj);

        int code = obj.getIntValue("code", -1);
        if (code != 0) {
            throw new IOException("创建任务失败，code=" + code + ", msg=" + obj.getString("msg"));
        }
        String data = obj.getString("data");
        if (data == null) {
            throw new IOException("响应中缺少 data.id");
        }
        return data;
    }

    /**
     * 暂停任务（修正返回值判断）
     */
    public boolean pauseTask(String taskId) throws IOException {
        String resp = post("/tasks/" + taskId + "/pause", "");
        JSONObject obj = JSONObject.parseObject(resp);
        return obj.getIntValue("code", -1) == 0;
    }

    /**
     * 恢复任务（同暂停）
     */
    public boolean resumeTask(String taskId) throws IOException {
        String resp = post("/tasks/" + taskId + "/resume", "");
        JSONObject obj = JSONObject.parseObject(resp);
        return obj.getIntValue("code", -1) == 0;
    }

    /**
     * 删除任务
     */
    public boolean deleteTask(String taskId) throws IOException {
        // DELETE 请求，Jsoup 需要通过 method 指定
        Connection.Response response = Jsoup.connect(baseUrl + "/tasks/" + taskId)
                .ignoreContentType(true)
                .method(Connection.Method.DELETE)
                .execute();
        return response.statusCode() == 200;
    }

    // ========== 数据类 ==========

    /**
     * 获取所有任务列表
     */
    public List<TaskInfo> listTasks() throws IOException {
        String resp = get("/tasks");
        JSONObject obj = JSONObject.parseObject(resp);
        logger.info(obj);
        // 检查响应码
        int code = obj.getIntValue("code", -1);
        if (code != 0) {
            throw new IOException("获取任务列表失败，code=" + code + ", msg=" + obj.getString("message"));
        }

        JSONObject data = obj.getJSONObject("data");
        if (data == null) {
            return new ArrayList<>();
        }

        JSONArray tasksArray = data.getJSONArray("tasks");
        if (tasksArray == null) {
            return new ArrayList<>();
        }

        List<TaskInfo> list = new ArrayList<>();
        for (int i = 0; i < tasksArray.size(); i++) {
            JSONObject t = tasksArray.getJSONObject(i);
            TaskInfo info = new TaskInfo();
            info.id = t.getString("id");
            info.name = t.getString("name");
            info.status = t.getString("status");

            try {
                info.progress = t.getDouble("progress");
                info.speed = t.getLong("speed");
                info.downloaded = t.getLong("downloaded");
                info.total = t.getLong("total");

            } catch (Exception e) {
                info.progress = 0;
                info.speed = 0;
                info.downloaded = 0;
                info.total = 0;
            }
            list.add(info);
        }
        return list;
    }

    // ========== 测试 ==========

    public static class TaskInfo {
        public String id;
        public String name;
        public double progress;      // 0~100
        public long speed;           // 字节/秒
        public long downloaded;      // 已下载字节
        public long total;           // 总字节
        public String status;        // "running", "paused", "done", "error"

        private static String formatSize(long bytes) {
            if (bytes < 1024) return bytes + "B";
            if (bytes < 1024 * 1024) return String.format("%.1fKB", bytes / 1024.0);
            if (bytes < 1024 * 1024 * 1024) return String.format("%.1fMB", bytes / (1024.0 * 1024));
            return String.format("%.2fGB", bytes / (1024.0 * 1024 * 1024));
        }

        private static String formatSpeed(long bytes) {
            if (bytes < 1024) return bytes + "B/s";
            if (bytes < 1024 * 1024) return String.format("%.1fKB/s", bytes / 1024.0);
            return String.format("%.1fMB/s", bytes / (1024.0 * 1024));
        }

        public String getProgressStr() {
            return String.format("%.1f%%", progress);
        }

        public String getSpeedStr() {
            return formatSpeed(speed);
        }

        public String getDownloadedStr() {
            return formatSize(downloaded);
        }

        public String getTotalStr() {
            return formatSize(total);
        }

        @Override
        public String toString() {
            return String.format("[%s] %s %s (%.1f%%) %s/%s @ %s",
                    status, id, name, progress,
                    getDownloadedStr(), getTotalStr(), getSpeedStr());
        }
    }
}
