package com.wmp.downloader.tools.file;

import com.wmp.downloader.tools.StringFormat;
import com.wmp.downloader.tools.ui.ToastMessage;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

public class FileOperation {

    public static final Logger logger = Logger.getLogger(FileOperation.class);

    /**
     * 复制文件或文件夹
     * @param source 源文件或文件夹
     * @param target 目标文件或文件夹（若 target 为已存在的目录，则会在其下创建与 source 同名的子项）
     * @return 复制成功返回 true，否则返回 false
     */
    public static boolean copy(File source, File target) {
        if (source == null || target == null || !source.exists()) {
            ToastMessage.show(
                    String.format(
                            StringFormat.translate("file_operation.copy_failed"),
                            "None"
                    ), ToastMessage.ERROR
            );
            return false;
        }

        try {
            Path srcPath = source.toPath();
            Path tgtPath = target.toPath();

            // 如果目标是一个已存在的目录，则在其下创建同名的子项
            if (Files.isDirectory(tgtPath)) {
                tgtPath = tgtPath.resolve(source.getName());
            }

            if (source.isDirectory()) {
                copyDirectory(srcPath, tgtPath);
            } else {
                copyFile(srcPath, tgtPath);
            }
            return true;
        } catch (IOException e) {
            logger.error("复制失败", e);
            ToastMessage.show(
                    String.format(
                            StringFormat.translate("file_operation.copy_failed"),
                            source.getName()
                    ), ToastMessage.ERROR
            );
            return false;
        }
    }

    /**
     * 复制单个文件（如果目标父目录不存在则自动创建）
     */
    private static void copyFile(Path source, Path target) throws IOException {
        // 确保目标父目录存在
        if (target.getParent() != null && !Files.exists(target.getParent())) {
            Files.createDirectories(target.getParent());
        }
        // 使用 REPLACE_EXISTING 覆盖已有文件，并保留文件属性
        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
    }

    /**
     * 递归复制整个目录及其内容
     */
    private static void copyDirectory(Path source, Path target) throws IOException {
        // 如果目标已存在且不是目录，则抛出异常（由调用者处理）
        if (Files.exists(target) && !Files.isDirectory(target)) {
            throw new IOException("目标已存在且不是一个目录: " + target);
        }

        // 遍历源目录树
        Files.walkFileTree(source, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                // 计算相对于源目录的路径
                Path relative = source.relativize(dir);
                Path dest = target.resolve(relative);

                // 如果目标目录不存在，则创建
                if (!Files.exists(dest)) {
                    Files.createDirectories(dest);
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Path relative = source.relativize(file);
                Path dest = target.resolve(relative);

                // 确保父目录存在（实际上 preVisitDirectory 已经创建了所有目录，但安全起见再次检查）
                if (dest.getParent() != null && !Files.exists(dest.getParent())) {
                    Files.createDirectories(dest.getParent());
                }

                // 复制文件（覆盖已存在的文件）
                Files.copy(file, dest, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                // 遇到无法访问的文件时，可以选择继续或抛出异常
                // 这里将异常重新抛出，由上层捕获
                throw exc;
            }
        });
    }
}
