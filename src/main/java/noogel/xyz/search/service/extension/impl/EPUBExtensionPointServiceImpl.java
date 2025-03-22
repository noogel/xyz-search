package noogel.xyz.search.service.extension.impl;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import noogel.xyz.search.infrastructure.consts.FileExtEnum;
import noogel.xyz.search.infrastructure.consts.FileProcessClassEnum;
import noogel.xyz.search.infrastructure.dto.ResRelationInfoDto;
import noogel.xyz.search.infrastructure.dto.dao.ChapterDto;
import noogel.xyz.search.infrastructure.dto.dao.FileResContentDto;
import noogel.xyz.search.infrastructure.dto.dao.FileResReadDto;
import noogel.xyz.search.infrastructure.exception.ExceptionCode;
import noogel.xyz.search.infrastructure.utils.FileHelper;
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
public class EPUBExtensionPointServiceImpl extends AbstractExtensionPointService {

    @Getter
    private final FileProcessClassEnum fileClass = FileProcessClassEnum.EPUB;

    @Getter
    private final Set<FileExtEnum> supportParseFileExtension = Set.of(FileExtEnum.EPUB);
    private final Set<FileExtEnum> subSupportFileExtension = Set.of(
            FileExtEnum.HTML, FileExtEnum.XHTML, FileExtEnum.XML
    );

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
                    FileExtEnum ext = FileHelper.getFileExtension(t.getAbsolutePath());
                    // 解析添加
                    if (subSupportFileExtension.contains(ext)) {
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
        ResRelationInfoDto resRel = autoFindRelationInfo(file);
        String title = Optional.ofNullable(resRel).map(ResRelationInfoDto::getTitle).orElse(null);
        return FileResContentDto.of(chapters).metaData("metaTitle", title);
    }
}
