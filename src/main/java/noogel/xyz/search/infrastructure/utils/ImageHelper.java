package noogel.xyz.search.infrastructure.utils;

import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Objects;

import static org.opencv.imgcodecs.Imgcodecs.imread;

public class ImageHelper {

    static {
        nu.pattern.OpenCV.loadLocally();
    }

    public static byte[] genThumbnailToByteArray(String filepath, Double width, Double height) throws IOException {
        Mat src = imread(filepath);
        Mat dest = new Mat();
        Size scaleSize = calResize(src.size(), width, height);
        Imgproc.resize(src, dest, scaleSize, 0, 0, Imgproc.INTER_AREA);
        BufferedImage bufferedImage = toBufferedImage(dest);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(bufferedImage, "jpg", outputStream);
        return outputStream.toByteArray();
    }

    private static byte[] toByteList(Mat matrix) {
        int bufferSize = matrix.channels() * matrix.cols() * matrix.rows();
        byte[] buffer = new byte[bufferSize];
        matrix.get(0, 0, buffer); // get all pixel from martix
        return buffer;
    }

    private static BufferedImage toBufferedImage(Mat matrix) {
        byte[] buffer = toByteList(matrix);
        int type = BufferedImage.TYPE_BYTE_GRAY;
        if (matrix.channels() > 1) {
            type = BufferedImage.TYPE_3BYTE_BGR;
        }
        BufferedImage image = new BufferedImage(matrix.cols(), matrix.rows(), type);
        final byte[] targetPixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
        System.arraycopy(buffer, 0, targetPixels, 0, buffer.length);
        return image;
    }

    private static Size calResize(Size src, Double width, Double height) {
        if (Objects.nonNull(width) && Objects.nonNull(height)) {
            return new Size(width, height);
        } else if (Objects.nonNull(width)) {
            return new Size(width, width / src.width * src.height);
        } else if (Objects.nonNull(height)) {
            return new Size(height / src.height * src.width, height);
        } else {
            return src;
        }
    }
}
