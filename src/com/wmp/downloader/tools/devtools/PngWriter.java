package com.wmp.downloader.tools.devtools;

import javax.imageio.ImageIO;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.CRC32;
import java.util.zip.Deflater;

/**
 * 极简 PNG 编码器（RGBA，无压缩优化）。
 *
 * <h3>为什么不用 {@link ImageIO}</h3>
 * <p>自检一开始用的是 {@code ImageIO.write(image, "png", file)}，结果抛
 * <pre>ClassNotFoundException: com.twelvemonkeys.imageio.spi.ImageReaderSpiBase</pre>
 * 原因不是代码有问题，而是<b>本项目的 classpath 有一个洞</b>：
 * {@code .idea/libraries/imageio_plugins_3_13_1.xml} 引用了兄弟项目
 * {@code DownLoader_plugin/Image_Converter/lib} 下的十二猴子<b>格式插件</b>
 * （bmp / tiff / webp / …），却没有引用它们共同依赖的 {@code imageio-core}。
 * {@code ImageIO} 首次使用时会扫描 classpath 上的 SPI，插件类加载不到父类就抛异常，
 * 于是<b>任何</b> ImageIO 调用都会失败——只是主项目自己从不使用 ImageIO，所以一直没暴露。</p>
 *
 * <p>自检工具不能依赖这种不可控的环境：在开发者的 IDE 里跑同样会炸。
 * 所以这里自带一个只做「写 PNG」这一件事的编码器，零依赖、行为确定。</p>
 *
 * <p>格式上做了必要的取舍：每个扫描行用 filter 0（None）、DEFLATE 用
 * {@link Deflater#BEST_SPEED}。自检截图追求的是「能看、不糊」，不是最小体积。</p>
 */
public final class PngWriter {

    private PngWriter() {
    }

    private static final byte[] SIGNATURE = {
            (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A
    };

    /**
     * 把图像写成 PNG 文件。
     *
     * @param image 源图（按 ARGB 读取，缺失的 alpha 视为不透明）
     * @param file  输出文件
     * @throws IOException 写文件失败
     */
    public static void write(java.awt.image.BufferedImage image, File file) throws IOException {
        int width = image.getWidth();
        int height = image.getHeight();

        // 原始数据：每行 = 1 字节过滤类型 + width 个 RGBA 像素
        byte[] raw = new byte[height * (1 + width * 4)];
        int p = 0;
        for (int y = 0; y < height; y++) {
            raw[p++] = 0;                       // filter: None
            for (int x = 0; x < width; x++) {
                int argb = image.getRGB(x, y);
                raw[p++] = (byte) (argb >> 16);   // R
                raw[p++] = (byte) (argb >> 8);    // G
                raw[p++] = (byte) argb;           // B
                raw[p++] = (byte) (argb >>> 24);  // A
            }
        }

        byte[] compressed = deflate(raw);

        try (DataOutputStream out = new DataOutputStream(
                new BufferedOutputStream(new FileOutputStream(file)))) {
            out.write(SIGNATURE);
            writeChunk(out, "IHDR", ihdr(width, height));
            writeChunk(out, "IDAT", compressed);
            writeChunk(out, "IEND", new byte[0]);
        }
    }

    /** 用一份可读的说明描述当前环境能否使用 ImageIO（供自检报告引用） */
    public static String describeImageIoAvailability() {
        try {
            java.util.Iterator<javax.imageio.ImageWriter> it = ImageIO.getImageWritersByFormatName("png");
            return it.hasNext() ? "可用" : "无可用 PNG 写出器";
        } catch (Throwable t) {
            return "不可用（" + t.getClass().getSimpleName() + ": " + t.getMessage()
                    + "）—— 疑与 classpath 缺少 imageio-core 有关，自检已改用内置编码器";
        }
    }

    // ==================================================================
    // 内部
    // ==================================================================

    private static byte[] ihdr(int width, int height) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream(13);
        try (DataOutputStream d = new DataOutputStream(buffer)) {
            d.writeInt(width);
            d.writeInt(height);
            d.writeByte(8);    // 位深
            d.writeByte(6);    // 颜色类型 6 = RGBA
            d.writeByte(0);    // 压缩方法：只有 0 合法
            d.writeByte(0);    // 过滤方法：只有 0 合法
            d.writeByte(0);    // 隔行扫描：0 = 不隔行
        }
        return buffer.toByteArray();
    }

    private static void writeChunk(DataOutputStream out, String type, byte[] data) throws IOException {
        byte[] typeBytes = type.getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        out.writeInt(data.length);
        out.write(typeBytes);
        out.write(data);

        CRC32 crc = new CRC32();
        crc.update(typeBytes);
        crc.update(data);
        out.writeInt((int) crc.getValue());
    }

    private static byte[] deflate(byte[] raw) {
        Deflater deflater = new Deflater(Deflater.BEST_SPEED);
        try {
            deflater.setInput(raw);
            deflater.finish();
            ByteArrayOutputStream out = new ByteArrayOutputStream(Math.max(64, raw.length / 4));
            byte[] buffer = new byte[16384];
            while (!deflater.finished()) {
                int n = deflater.deflate(buffer);
                out.write(buffer, 0, n);
            }
            return out.toByteArray();
        } finally {
            deflater.end();
        }
    }
}
