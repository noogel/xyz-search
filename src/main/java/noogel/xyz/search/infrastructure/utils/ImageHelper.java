package noogel.xyz.search.infrastructure.utils;

import org.apache.commons.lang3.tuple.Pair;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Objects;

public class ImageHelper {

    public static byte[] genThumbnailToByteArray(String originalImagePath, Integer width, Integer height) throws IOException {
        // 读取原始图像
        File originalImageFile = new File(originalImagePath);
        BufferedImage originalImage = ImageIO.read(originalImageFile);
        // 缩略图的宽度和高度
        Pair<Double, Double> newSize = calResize(
                Pair.of((double) originalImage.getWidth(), (double) originalImage.getHeight()),
                Objects.nonNull(width) ? (double) width : null,
                Objects.nonNull(height) ? (double) height : null
        );

        int thumbWidth = newSize.getLeft().intValue();
        int thumbHeight = newSize.getRight().intValue();

        // 创建缩略图图像
        BufferedImage bufferedImage = new BufferedImage(thumbWidth, thumbHeight, BufferedImage.TYPE_3BYTE_BGR);

        // 创建Graphics2D对象
        Graphics2D graphics = bufferedImage.createGraphics();

        // 设置缩放质量
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        // 绘制图像
        graphics.drawImage(originalImage, 0, 0, thumbWidth, thumbHeight, null);

        // 释放资源
        graphics.dispose();

        // 写入缩略图
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(bufferedImage, "jpg", baos);
            return baos.toByteArray();
        }
    }

    private static Pair<Double, Double> calResize(Pair<Double, Double> src, Double width, Double height) {
        if (Objects.nonNull(width) && Objects.nonNull(height)) {
            return Pair.of(width, height);
        } else if (Objects.nonNull(width)) {
            return Pair.of(width, width / src.getLeft() * src.getRight());
        } else if (Objects.nonNull(height)) {
            return Pair.of(height / src.getRight() * src.getLeft(), height);
        } else {
            return src;
        }
    }
}
