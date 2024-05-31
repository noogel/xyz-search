package noogel.xyz.search.service.extension.impl;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import noogel.xyz.search.infrastructure.dto.ResRelationInfoDto;
import noogel.xyz.search.infrastructure.dto.dao.ChapterDto;
import noogel.xyz.search.infrastructure.dto.dao.FileResContentDto;
import noogel.xyz.search.infrastructure.dto.dao.FileResReadDto;
import noogel.xyz.search.infrastructure.exception.ExceptionCode;
import noogel.xyz.search.infrastructure.utils.FileHelper;
import noogel.xyz.search.service.extension.ExtensionPointService;
import noogel.xyz.search.service.extension.ExtensionUtilsService;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@Service
@Slf4j
public class EPUBExtensionPointServiceImpl implements ExtensionPointService {

    private static final Set<String> SUPPORT = Set.of("epub");
    private static final Set<String> SUB_SUPPORT = Set.of("html", "xhtml", "xml");

    @Resource
    private ExtensionUtilsService extensionUtilsService;

    @Override
    public boolean supportFile(String filePath) {
        return extensionUtilsService.supportFileExtension(SUPPORT, filePath);
    }

    private List<ChapterDto> parseFileToChapters(File zipFile) {
        List<ChapterDto> resp = new ArrayList<>();
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
            // 扫描内容
            // 需要按照目录名称排序
            List<File> subFiles = FileHelper.parseAllSubFiles(tmp);
            for (File t : subFiles) {
                if (t.isFile()) {
                    String ext = FileHelper.getFileExtension(t.getAbsolutePath());
                    // 解析添加
                    if (SUB_SUPPORT.contains(ext)) {
                        resp.add(ChapterDto.of(t.getName(), Jsoup.parse(t).text()));
                    }
                }
            }
        } catch (IOException e) {
            throw ExceptionCode.FILE_ACCESS_ERROR.throwExc(e);
        } finally {
            // 删除临时目录
            if (Objects.nonNull(tmp)) {
                FileHelper.deleteFile(tmp);
            }
        }
        return resp;
    }

    @Override
    public FileResContentDto parseFile(FileResReadDto resReadDto) {
        File file = resReadDto.genFile();
        List<ChapterDto> chapters = parseFileToChapters(file);
        ResRelationInfoDto resRel = extensionUtilsService.autoFindRelationInfo(file);
        String title = Optional.ofNullable(resRel).map(ResRelationInfoDto::getTitle).orElse(null);
        return FileResContentDto.of(chapters, title);
    }
}
