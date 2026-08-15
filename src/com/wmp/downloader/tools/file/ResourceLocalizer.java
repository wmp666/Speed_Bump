package com.wmp.downloader.tools.file;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * 资源本地化工具类（从Kotlin转换）
 */
public class ResourceLocalizer {

    private static final Logger logger = Logger.getLogger(ResourceLocalizer.class);

    /**
     * 将内置文件复制到指定目录
     *
     * @param outputPath 输出目录
     * @param inputPath  资源路径（类路径下的目录）
     */
    @SuppressWarnings("SameParameterValue")
    public static void copyEmbeddedFile(String outputPath, String inputPath) {
        File dir = new File(outputPath);
        if (!dir.getParentFile().exists()) {
            boolean created = dir.getParentFile().mkdirs();
            if (!created) {
                logger.error("无法创建输出目录: "+outputPath);
                return;
            }
        }

        try (InputStream is = ResourceLocalizer.class.getResourceAsStream(inputPath)) {
            if (is == null) {
                logger.error("内置文件[" + inputPath + "]未找到，资源路径: " +
                        ResourceLocalizer.class.getResource(inputPath));
                return;
            }

            Files.copy(is, Paths.get(outputPath), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            logger.error("文件[" + inputPath + "]本地化失败", e);
        }
    }
}