package noogel.xyz.search.infrastructure.utils;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
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
}
