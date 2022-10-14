package noogel.xyz.search.infrastructure.utils;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Slf4j
public class FileHelper {
    /**
     * 转换文件大小
     */
    public static String formatFileSize(long fileS) {
        DecimalFormat df = new DecimalFormat("#.00");
        String fileSizeString = "";
        String wrongSize = "0B";
        if (fileS == 0) {
            return wrongSize;
        }
        if (fileS < 1024) {
            fileSizeString = df.format((double) fileS) + "B";
        } else if (fileS < 1048576) {
            fileSizeString = df.format((double) fileS / 1024) + "KB";
        } else if (fileS < 1073741824) {
            fileSizeString = df.format((double) fileS / 1048576) + "MB";
        } else {
            fileSizeString = df.format((double) fileS / 1073741824) + "GB";
        }
        return fileSizeString;
    }

    /**
     * 获取文件扩展
     *
     * @param path
     * @return
     */
    public static String getFileExtension(String path) {
        String extension = "";
        int i = path.lastIndexOf('.');
        if (i > 0) {
            extension = path.substring(i + 1).toLowerCase();
        }
        return extension.toLowerCase();
    }

    /**
     * 获取所有子文件和子文件夹
     *
     * @param file
     */
    public static List<File> parseAllSubFiles(File file) {
        if (!file.isDirectory()) {
            return Collections.emptyList();
        }
        // 便利文件和文件夹
        File[] subFiles = file.listFiles();
        if (Objects.isNull(subFiles)) {
            return Collections.emptyList();
        }
        List<File> resp = new ArrayList<>();
        for (File subFile : subFiles) {
            resp.add(subFile);
            if (subFile.isDirectory()) {
                resp.addAll(parseAllSubFiles(subFile));
            }
        }
        return resp;
    }

    /**
     * 删除文件或目录
     *
     * @param file
     */
    public static void deleteFile(File file) {
        if (file.exists()) {//判断文件是否存在
            if (file.isFile()) {//判断是否是文件
                file.delete();//删除文件
            } else if (file.isDirectory()) {//否则如果它是一个目录
                File[] files = file.listFiles();//声明目录下所有的文件 files[];
                if (Objects.nonNull(files)) {
                    for (int i = 0; i < files.length; i++) {//遍历目录下所有的文件
                        deleteFile(files[i]);//把每个文件用这个方法进行迭代
                    }
                }
                file.delete();//删除文件夹
            }
        }
    }

    public static Charset detectCharset(File file) {
        Charset charset = Charset.forName("GBK");
        byte[] first3Bytes = new byte[3];
        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file))) {
            boolean checked = false;
            bis.mark(0);
            int read = bis.read(first3Bytes, 0, 3);
            if (read == -1) {
                //文件编码为 ANSI
                return charset;
            } else if (first3Bytes[0] == (byte) 0xFF && first3Bytes[1] == (byte) 0xFE) {
                //文件编码为 Unicode
                charset = StandardCharsets.UTF_16LE;
                checked = true;
            } else if (first3Bytes[0] == (byte) 0xFE && first3Bytes[1] == (byte) 0xFF) {
                //文件编码为 Unicode big endian
                charset = StandardCharsets.UTF_16BE;
                checked = true;
            } else if (first3Bytes[0] == (byte) 0xEF && first3Bytes[1] == (byte) 0xBB
                    && first3Bytes[2] == (byte) 0xBF) {
                //文件编码为 UTF-8
                charset = StandardCharsets.UTF_8;
                checked = true;
            }
            bis.reset();
            if (!checked) {
                int loc = 0;
                while ((read = bis.read()) != -1) {
                    loc++;
                    if (read >= 0xF0) {
                        break;
                    }
                    // 单独出现BF以下的，也算是GBK
                    if (0x80 <= read && read <= 0xBF) {
                        break;
                    }
                    if (0xC0 <= read && read <= 0xDF) {
                        read = bis.read();
                        // 双字节 (0xC0 - 0xDF)
                        if (0x80 <= read && read <= 0xBF) {
                            // (0x80 - 0xBF),也可能在GB编码内
                            continue;
                        } else {
                            break;
                        }
                    } else if (0xE0 <= read && read <= 0xEF) {// 也有可能出错，但是几率较小
                        read = bis.read();
                        if (0x80 <= read && read <= 0xBF) {
                            read = bis.read();
                            if (0x80 <= read && read <= 0xBF) {
                                charset = StandardCharsets.UTF_8;
                                break;
                            } else {
                                break;
                            }
                        } else {
                            break;
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return charset;
    }
}
