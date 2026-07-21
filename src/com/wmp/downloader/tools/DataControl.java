package com.wmp.downloader.tools;

import com.alibaba.fastjson.JSON;
import org.apache.log4j.DailyRollingFileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class DataControl {
    public static final ArrayList<String> themeList = new ArrayList<>();
    private static final Logger logger = Logger.getLogger(DataControl.class);
    private static final Path DATA_DIR = Paths.get(
            System.getProperty("user.home"), ".w-downloader"
    );
    private static final Path DATA_FILE = DATA_DIR.resolve("data.json");
    //真正被保存的数据
    private static final HashMap<String, Object> saveData = new HashMap<>();
    private static final HashMap<String, Object> data = new HashMap<>();

    static {
        themeList.addAll(List.of("Mac Light", "Mac Dark", "Light", "Dark", "Darcula", "IntelliJ", "System", "Windows Classic", "Metal"));
    }

    public static void configureLogPath(String logDir) throws IOException {
        new File(logDir).mkdirs();

        Logger rootLogger = Logger.getRootLogger();

        PatternLayout layout = new PatternLayout("[%-5p] %d{yyyy-MM-dd HH:mm:ss,SSS} method:%l%n%m%n");

        DailyRollingFileAppender fileAppender = new DailyRollingFileAppender(layout, logDir + "/app.log", "'.'yyyy-MM-dd");
        fileAppender.setThreshold(Level.DEBUG);
        rootLogger.addAppender(fileAppender);

        DailyRollingFileAppender errorAppender = new DailyRollingFileAppender(layout, logDir + "/error.log", "'.'yyyy-MM-dd");
        errorAppender.setThreshold(Level.ERROR);
        rootLogger.addAppender(errorAppender);
    }

    public static void load() {

        try {
            configureLogPath(getDataPath().getAbsolutePath());
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "日志路径配置失败\n因此出现问题后无法查看日志", "错误", JOptionPane.ERROR_MESSAGE);
        }

        logger.debug("加载数据...");
        try {
            if (Files.exists(DATA_FILE)) {
                String data = Files.readString(DATA_FILE, StandardCharsets.UTF_8);
                var jsonData = JSON.parseObject(data);

                DataControl.saveData.putAll(jsonData.getInnerMap());
                DataControl.data.putAll(jsonData.getInnerMap());
            }
            logger.debug("文本数据加载完成");

            //对数据进一步处理
            var tempDataMap = new HashMap<String, Object>();
            DataControl.saveData.forEach((key, value) -> {
                initProcessingData(key, value, tempDataMap);
            });
            DataControl.data.putAll(tempDataMap);

        } catch (IOException e) {
            logger.error("加载数据失败", e);
        }
    }

    /**
     * 获取数据
     *
     * @param key          键
     * @param defaultValue 用户设置的默认值
     * @return 值，如果不存在则返回 defaultValue
     */
    public static <T> T get(String key, T defaultValue) {
        return (T) data.getOrDefault(key, defaultValue);
    }

    public static void put(String key, Object value) {
        saveData.put(key, value);
        data.put(key, value);

        //判断添加的数据是否有会导致其他值需做出改变，并修改
        var tempDataMap = new HashMap<String, Object>();
        initProcessingData(key, value, tempDataMap);
        DataControl.data.putAll(tempDataMap);
    }

    /**
     * 刷新数据,用于当部分软件数据改变而没有发生数据存储行为时,更新加工数据
     */
    public static void refresh() {
        var tempDataMap = new HashMap<String, Object>();
        saveData.forEach((key, value) -> {
            initProcessingData(key, value, tempDataMap);
        });
        DataControl.data.putAll(tempDataMap);
    }

    private static void initProcessingData(String key, Object value, HashMap<String, Object> tempDataMap) {
        tempDataMap.put("version", "0.0.4");
        if (key.equals("theme")) {
            if (!EasterEggData.canUseFlatLaf) {
                tempDataMap.put("theme_type", "light");
                return;
            }
            // 处理主题数据
            switch (value.toString()) {
                case "Mac Dark", "Dark", "Darcula" -> {
                    tempDataMap.put("theme_type", "dark");
                }
                case "Mac Light", "Light", "IntelliJ", "System", "Windows Classic", "Metal" -> {
                    tempDataMap.put("theme_type", "light");
                }
            }
        }
    }

    public static void save() {
        try {
            Files.createDirectories(DATA_DIR);
            String json = JSON.toJSONString(saveData);
            Files.writeString(DATA_FILE, json, StandardCharsets.UTF_8, Files.exists(DATA_FILE) ? StandardOpenOption.TRUNCATE_EXISTING : StandardOpenOption.CREATE);
        } catch (IOException e) {
            logger.error("保存数据失败", e);
        }
        logger.debug("数据保存完成");
    }

    public static File getDownloadFilePath(){
        return new File(data.getOrDefault("DownloadFilePath", new File(getDataPath(), "Download")).toString());
    }


    public static File getDataPath() {
        return DATA_DIR.toFile();
    }

    public static File getTempPath() {
        var file = data.containsKey("TempFilePath") ? new File(data.get("TempFilePath").toString()) : getDefaultTempPath();
        return file;
    }

    public static void deleteFolder(File file){
        //删除整个文件夹里的内容
        try {
            Files.walk(Paths.get(file.toURI()))
                    .sorted((o1, o2) -> -o1.compareTo(o2))
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException ex) {
                            logger.error("删除失败", ex);
                        }
                    });
            JOptionPane.showMessageDialog(null, "删除成功");
        } catch (IOException e) {
            logger.error("删除文件夹失败", e);
        }
    }

    public static File getDefaultTempPath() {
        return new File(System.getProperty("java.io.tmpdir"), "speed-bump");
    }
}
