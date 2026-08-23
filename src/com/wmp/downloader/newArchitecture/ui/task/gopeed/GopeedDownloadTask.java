package com.wmp.downloader.newArchitecture.ui.task.gopeed;

import com.alibaba.fastjson2.JSONObject;
import com.wmp.downloader.newArchitecture.abstractTask.AbstractTask;
import com.wmp.downloader.newArchitecture.abstractTask.downloadTask.StatusTipPanel;
import com.wmp.downloader.newArchitecture.exception.DownloadException;
import com.wmp.downloader.tools.file.DataControl;
import com.wmp.downloader.tools.StringFormat;
import com.wmp.downloader.tools.download.URLDownloadTool;
import com.wmp.downloader.tools.ui.ToastMessage;
import com.wmp.downloader.tools.ui.UITools;
import com.wmp.downloader.ui.FunctionDialog;
import org.apache.log4j.Logger;
import org.jsoup.Connection;
import org.jsoup.Jsoup;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;

public class GopeedDownloadTask extends AbstractTask {

    private static final Logger logger = Logger.getLogger(GopeedDownloadTask.class);

    private final String baseUrl;
    private final JProgressBar progressBar = new JProgressBar(0, 100);
    private final String content;
    private String taskID = "";
    private long fileSize;

    private final StatusTipPanel DOWNLOAD_SIZE_PANEL = StatusTipPanel.DOWNLOAD_SIZE_CREATOR.create();
    private final StatusTipPanel DOWNLOAD_SPEED_PANEL = StatusTipPanel.DOWNLOAD_SPEED_CREATOR.create();
    private final StatusTipPanel SHARE_SIZE_PANEL = StatusTipPanel.SHARE_SIZE_CREATOR.create();
    private final StatusTipPanel SHARE_SPEED_PANEL = StatusTipPanel.SHARE_SPEED_CREATOR.create();

    private final Timer infoUpdateTimer = new Timer(500, _ -> {
        var body = getBody("/tasks/" + taskID, null, Connection.Method.GET);
        logger.info(body);
        try {
            var taskInfo = JSONObject.parseObject(body);
            if (taskInfo.getIntValue("code", -1) != 0) {
                logger.error("获取gopeed数据异常");
                isStart = true;
                return;
            }
            //数据没有问题，开始解析
            //数据结构：data -> progress -> used(所需时间) downloaded(下载总大小) speed(下载速度) uploaded(上传总大小) uploadSpeed(上传速度) extractProgress(进度)
            //data -> meta -> res -> size


            var data = taskInfo.getJSONObject("data");

            var status = data.getString("status");

            fileSize = data.getJSONObject("meta").getJSONObject("res").getLongValue("size", fileSize);
            var progress = data.getJSONObject("progress");

            var downloaded = progress.getLongValue("downloaded", 0);
            var speed = progress.getLongValue("speed", 0);
            var uploaded = progress.getLongValue("uploaded", 0);
            var uploadSpeed = progress.getLongValue("uploadSpeed", 0);
            var extractProgress = (downloaded * 100) / fileSize;
            progressBar.setStringPainted(false);
            if (fileSize <= 0) progressBar.setIndeterminate(true);
            else {
                progressBar.setIndeterminate(false);
                progressBar.setValue((int) extractProgress);
            }

            DOWNLOAD_SIZE_PANEL.setText(URLDownloadTool.DownloadProgress.formatSize(downloaded));
            DOWNLOAD_SPEED_PANEL.setText(URLDownloadTool.DownloadProgress.formatSize(speed) + "/s");
            SHARE_SIZE_PANEL.setText(URLDownloadTool.DownloadProgress.formatSize(uploaded));
            SHARE_SPEED_PANEL.setText(URLDownloadTool.DownloadProgress.formatSize(uploadSpeed) + "/s");


        } catch (Exception e) {
            logger.error("json数据解析异常", e);
            isStart = true;
            return;
        }
    });


    public GopeedDownloadTask(JSONObject jsonObject) {
        super(jsonObject);
        this.content = jsonObject.getString("url");
        this.fileSize = jsonObject.getLongValue("size", 0);
        this.baseUrl = "http://127.0.0.1:" + DataControl.get("gopeed_port", 9999) + "/api/v1";

        //检测gopeed是否启动，判断能否连接
        ensureGopeedRunning(baseUrl, DataControl.get("gopeed_path", ""));

        addStatusTips(DOWNLOAD_SIZE_PANEL, DOWNLOAD_SPEED_PANEL, SHARE_SIZE_PANEL, SHARE_SPEED_PANEL);
    }

    public static boolean ensureGopeedRunning(String baseUrl, String gopeedExecutablePath) {
        if (isGopeedRunning(baseUrl)) {
            return true; // 已运行，直接返回
        }

        JPanel panel = new JPanel(new BorderLayout());
        JTextArea textArea = new JTextArea(StringFormat.translate("task", "task.gopeed_task.gopeed_run_failed.confirm"));
        panel.add(textArea);

        // 弹出询问框

        int choice = FunctionDialog.showDialog(null,
                StringFormat.translate("task", "task.gopeed_task.gopeed_run_failed"),
                panel, result -> {

                }, FunctionDialog.OK_CANCEL_BUTTONS, 0,
                null, 0
        );

        if (choice == FunctionDialog.RESULT_OK) {
            try {
                // 启动 Gopeed 进程（假设可执行文件在 PATH 中或给出了完整路径）
                ProcessBuilder pb = new ProcessBuilder(gopeedExecutablePath);
                pb.start();

                // 等待 3 秒让 Gopeed 完成初始化
                Thread.sleep(3000);

                // 再次检测
                if (isGopeedRunning(baseUrl)) {
                    ToastMessage.show(null, StringFormat.translate("task", "task.gopeed_task.try_to_start.success"), ToastMessage.SUCCESS);
                    return true;
                } else {
                    ToastMessage.show(null, StringFormat.translate("task", "task.gopeed_task.try_to_start.failed"), ToastMessage.ERROR);
                    return false;
                }
            } catch (Exception e) {
                ToastMessage.show(null, StringFormat.translate("task", "task.gopeed_task.try_to_start.error") + "\n" + e.getMessage(), ToastMessage.ERROR);
                return false;
            }
        }
        return false;
    }

    /**
     * 检查 Gopeed API 是否可访问
     *
     * @param baseUrl Gopeed 的 API 基础地址，例如 "<a href="http://127.0.0.1:9999/api/v1">http://127.0.0.1:9999/api/v1</a>"
     * @return true 表示连接成功，false 表示无法连接
     */
    public static boolean isGopeedRunning(String baseUrl) {
        try {
            // 尝试 GET /tasks 接口，超时 2 秒
            Connection.Response response = Jsoup.connect(baseUrl + "/tasks")
                    .ignoreContentType(true)
                    .timeout(2000)
                    .execute();
            // 只要没有抛出异常，就认为服务在运行（即使返回 401 或 400）
            return true;
        } catch (IOException e) {
            // 连接拒绝、超时等均为未启动
            return false;
        }
    }

    @Override
    public void doWhenExit() {

        //检测gopeed是否启动，判断能否连接
        if (!ensureGopeedRunning(baseUrl, DataControl.get("gopeed_path", ""))) {
            throw new DownloadException("gopeed启动失败");
        }

        var body = getBody("/tasks/" + taskID, null, Connection.Method.DELETE);
        logger.info(body);
        try {
            var taskInfo = JSONObject.parseObject(body);
            if (taskInfo.getIntValue("code", -1) != 0) {
                ToastMessage.show(this,
                        StringFormat.translate("task", "task.gopeed_task.task_delete_failed") +
                                " code=" + taskInfo.getIntValue("code"), ToastMessage.ERROR);
                logger.error("任务删除失败");

                throw new DownloadException("无法在gopeed中删除任务 code=" + taskInfo.getIntValue("code"));
            }
        } catch (Exception e) {
            ToastMessage.show(this, StringFormat.translate("task", "task.gopeed_task.task_delete_failed"), ToastMessage.ERROR);
            logger.error("json数据解析异常", e);
            //throw new DownloadException("无法解析gopeed的数据");
        }
        infoUpdateTimer.stop();
    }

    @Override
    public void doWhenStart() throws Exception {

        removeAllStatusTip();
        addStatusTips(DOWNLOAD_SIZE_PANEL, DOWNLOAD_SPEED_PANEL, SHARE_SIZE_PANEL, SHARE_SPEED_PANEL);

        //检测gopeed是否启动，判断能否连接
        if (!ensureGopeedRunning(baseUrl, DataControl.get("gopeed_path", ""))) {
            throw new DownloadException("gopeed启动失败");
        }

        //初次调用
        {
            var file = new File(savePath, fileName);
            if (file.exists()) {
                if (JOptionPane.showConfirmDialog(null,
                        StringFormat.translate("task", "task.download_task.delete_exists_file.confirm")) == JOptionPane.YES_OPTION) {
                    DataControl.delete(file, true);
                } else {
                    isStart = false;
                    throw new DownloadException("无法创建任务");
                }
            }

            JSONObject info = new JSONObject();
            info.put("req", JSONObject.of("url", content));
            JSONObject option = new JSONObject();
            option.put("name", fileName);
            option.put("path", savePath.getAbsolutePath());

            info.put("opts", option);
            var body = getBody("/tasks", info.toString(), Connection.Method.POST);
            logger.info(body);
            try {
                var taskInfo = JSONObject.parseObject(body);
                if (taskInfo.getIntValue("code", -1) != 0) {
                    ToastMessage.show(this,
                            StringFormat.translate("task", "task.gopeed_task.task_info_error") +
                                    " code=" + taskInfo.getIntValue("code"), ToastMessage.ERROR);
                    logger.error("获取gopeed数据异常");
                    isStart = false;
                    startCount = 0;
                    throw new DownloadException("无法创建任务 code=" + taskInfo.getIntValue("code"));
                }
                this.taskID = taskInfo.getString("data");
            } catch (Exception e) {

                logger.error("json数据解析异常", e);
                isStart = false;
                startCount = 0;
                throw new DownloadException("创建任务似乎失败，Json数据解析失败");
            }
        }
        ProgressBarsPanel.add(UITools.createProgressBarPanel(progressBar));
        infoUpdateTimer.start();
    }

    @Override
    public void doWhenRestart() throws Exception {

        //检测gopeed是否启动，判断能否连接
        if (!ensureGopeedRunning(baseUrl, DataControl.get("gopeed_path", ""))) {
            throw new DownloadException("gopeed启动失败");
        }

        //再次调用，重启下载
        if (!taskID.isBlank()) {
            var body = getBody("/tasks/" + taskID + "/continue", null, Connection.Method.PUT);
            logger.info(body);
            try {
                var taskInfo = JSONObject.parseObject(body);
                if (taskInfo.getIntValue("code", -1) != 0) {
                    ToastMessage.show(this,
                            StringFormat.translate("task", "task.gopeed_task.task_info_error") +
                                    " code=" + taskInfo.getIntValue("code"), ToastMessage.ERROR);
                    logger.error("获取gopeed数据异常");
                    isStart = false;
                    startCount = 0;
                    throw new DownloadException("重启任务失败 code=" + taskInfo.getIntValue("code"));
                }
            } catch (Exception e) {

                logger.error("json数据解析异常", e);
                isStart = false;
                startCount = 0;
                throw new DownloadException("重启任务失败，json数据解析异常");
            }
            infoUpdateTimer.start();
        } else {
            throw new NullPointerException("taskID值为空");
        }
    }

    @Override
    public void doWhenStop() {

        //检测gopeed是否启动，判断能否连接
        if (!ensureGopeedRunning(baseUrl, DataControl.get("gopeed_path", ""))) {
            throw new DownloadException("gopeed启动失败");
        }

        var body = getBody("/tasks/" + taskID + "/pause", null, Connection.Method.PUT);
        logger.info(body);
        try {
            var taskInfo = JSONObject.parseObject(body);
            if (taskInfo.getIntValue("code", -1) != 0) {
                ToastMessage.show(this,
                        StringFormat.translate("task", "task.gopeed_task.task_info_error") +
                                " code=" + taskInfo.getIntValue("code"), ToastMessage.ERROR);
                logger.error("获取gopeed数据异常");
                isStart = true;
                throw new DownloadException("任务暂停失败 code=" + taskInfo.getIntValue("code"));
            }
        } catch (Exception e) {

            logger.error("json数据解析异常", e);
            isStart = true;
            throw new DownloadException("任务暂停失败，json数据解析异常");
        }
        infoUpdateTimer.stop();

    }

    private String getBody(String modeStr, String jsonBody, Connection.Method method) {
        Connection.Response response = null;
        try {
            Connection conn = Jsoup.connect(baseUrl + modeStr)
                    .header("Content-Type", "application/json")
                    .ignoreContentType(true)
                    .timeout(30000)
                    .method(method);
            response = (jsonBody == null ? conn : conn.requestBody(jsonBody))
                    .execute();

        } catch (IOException e) {
            logger.error("获取gopeed数据异常", e);
            return "";
        }
        return response.body();
    }
}
