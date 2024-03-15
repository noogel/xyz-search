package noogel.xyz.search.application.controller.view;

import jakarta.annotation.Resource;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import noogel.xyz.search.infrastructure.config.CommonsConsts;
import noogel.xyz.search.infrastructure.exception.ExceptionCode;
import noogel.xyz.search.infrastructure.utils.FileHelper;
import noogel.xyz.search.service.SearchService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.Enumeration;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@Controller
public class EpubViewCtrl {

    private static final Map<String, File> TMP_DIRS = new ConcurrentHashMap<>();

    @Resource
    private SearchService searchService;

    /**
     * epub 专用展示页面
     *
     * @param book
     * @return
     */
    @RequestMapping(value = "/epub/web/view", method = RequestMethod.GET)
    public ModelAndView fileEpubView(@RequestParam(required = true) String book) {
        if (!TMP_DIRS.containsKey(book)) {
            String resourcePath = searchService.getDownloadResource(book).getAbsolutePath();
            TMP_DIRS.put(book, unzipEPub(book, new File(resourcePath)));
        }
        return new ModelAndView("epub/viewer");
    }

    /**
     * epub 专用展示页面
     *
     * @return
     */
    @RequestMapping(value = "/bibi-bookshelf/{resId}/**", method = RequestMethod.GET)
    public void bibiBookshelf(HttpServletRequest request, HttpServletResponse response, @PathVariable String resId) {
        String resourcePath = null;
        if (!TMP_DIRS.containsKey(resId)) {
            resourcePath = searchService.getDownloadResource(resId).getAbsolutePath();
            TMP_DIRS.put(resId, unzipEPub(resId, new File(resourcePath)));
        }
        String targetPath = request.getRequestURL().toString().split(resId)[1];
        String targetFullPath = TMP_DIRS.get(resId).getAbsolutePath() + targetPath;
        File targetFile = new File(targetFullPath);

        try (InputStream inputStream = new FileInputStream(targetFile)) {
            String contentType = FileHelper.getContentType(targetFile);
            response.reset();
            response.setContentType(contentType);
            try (ServletOutputStream outputStream = response.getOutputStream()) {
                byte[] b = new byte[1024];
                int len;
                while ((len = inputStream.read(b)) > 0) {
                    outputStream.write(b, 0, len);
                }
            }
        } catch (Exception ex) {
            throw ExceptionCode.FILE_ACCESS_ERROR.throwExc(ex);
        }

    }

    private File unzipEPub(String resId, File zipFile) {
        File tmp = null;
        try (ZipFile zip = new ZipFile(zipFile, Charset.defaultCharset())) {
            // 创建临时文件夹
            tmp = Files.createTempDirectory("tmp").toFile();
            // 解压缩 epub 文件
            for (Enumeration<? extends ZipEntry> entries = zip.entries(); entries.hasMoreElements(); ) {
                ZipEntry entry = entries.nextElement();
                String zipEntryName = entry.getName();
                try (InputStream in = zip.getInputStream(entry)) {
                    //指定解压后的文件夹+当前zip文件的名称
                    String outPath = (tmp.getAbsolutePath() + "/" + zipEntryName).replace("/", File.separator);
                    //判断路径是否存在,不存在则创建文件路径
                    File file = new File(outPath.substring(0, outPath.lastIndexOf(File.separator)));
                    if (!file.exists()) {
                        file.mkdirs();
                    }
                    // 判断文件全路径是否为文件夹,如果是上面已经上传,不需要解压
                    if (new File(outPath).isDirectory()) {
                        continue;
                    }
                    try (OutputStream out = new FileOutputStream(outPath)) {
                        byte[] buf1 = new byte[2048];
                        int len;
                        while ((len = in.read(buf1)) > 0) {
                            out.write(buf1, 0, len);
                        }
                    }
                }
            }
        } catch (IOException e) {
            // 删除临时目录
            if (Objects.nonNull(tmp)) {
                FileHelper.deleteFile(tmp);
            }
            throw ExceptionCode.FILE_ACCESS_ERROR.throwExc(e);
        }
        final File willDel = tmp;
        CommonsConsts.DELAY_EXECUTOR_SERVICE.schedule(() -> deleteTree(resId, willDel), 1, TimeUnit.HOURS);
        return tmp;
    }

    private void deleteTree(String resId, File tmp) {
        // 删除临时目录
        if (Objects.nonNull(tmp) && tmp.exists()) {
            FileHelper.deleteFile(tmp);
        }
        TMP_DIRS.remove(resId);
    }
}
