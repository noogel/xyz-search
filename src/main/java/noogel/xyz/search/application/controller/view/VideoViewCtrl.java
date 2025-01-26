package noogel.xyz.search.application.controller.view;

import jakarta.annotation.Resource;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import noogel.xyz.search.infrastructure.config.SearchPropertiesConfig;
import noogel.xyz.search.infrastructure.dto.ResourceDownloadDto;
import noogel.xyz.search.infrastructure.exception.ExceptionCode;
import noogel.xyz.search.infrastructure.utils.FileHelper;
import noogel.xyz.search.service.SearchService;
import org.apache.pdfbox.io.IOUtils;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

@RestController
public class VideoViewCtrl {
    @Resource
    private SearchPropertiesConfig.SearchConfig config;
    @Resource
    private SearchService searchService;

    /**
     * 视频渲染页面
     *
     * @param resId
     * @return
     */
    @RequestMapping(value = "/video/{resId}", method = RequestMethod.GET)
    public ModelAndView videoPage(@PathVariable String resId) {
        ResourceDownloadDto downloadResource = searchService.getDownloadResource(resId);
        String videoFilePath = downloadResource.getAbsolutePath();
        File file = new File(videoFilePath);
        ExceptionCode.FILE_ACCESS_ERROR.throwOn(!file.exists(), "资源不存在");
        ModelAndView mv = new ModelAndView("video");
        mv.addObject("resId", resId);
        mv.addObject("resTitle", downloadResource.getResTitle());
        return mv;
    }

    /**
     * 视频切片列表
     *
     * @param resId
     * @return
     * @throws IOException
     * @throws InterruptedException
     */
    @GetMapping(value = "/video/m3u8/{resId}", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<byte[]> getVideo(@PathVariable String resId) throws IOException, InterruptedException {
        ResourceDownloadDto downloadResource = searchService.getDownloadResource(resId);
        String videoFilePath = downloadResource.getAbsolutePath();
        File file = new File(videoFilePath);
        ExceptionCode.FILE_ACCESS_ERROR.throwOn(!file.exists(), "资源不存在");
        String tmpDir = config.getBase().getConfigFilePath() + "/tmp/" + resId;
        File tmp = new File(tmpDir);
        if (!tmp.exists()) {
            tmp.mkdirs();
        }
        String baseUrl = "/video/" + resId + "/ts/";
        String m3u8FilePath = tmpDir + "/output.m3u8";

        if (new File(m3u8FilePath).exists()) {
            // 返回M3U8文件给前端
            InputStreamResource resource = new InputStreamResource(new FileInputStream(m3u8FilePath));
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(IOUtils.toByteArray(resource.getInputStream()));
        }

        // 执行FFmpeg命令并获取输出流
        ProcessBuilder processBuilder = new ProcessBuilder("ffmpeg", "-i", videoFilePath, "-c:v", "libx264", "-hls_base_url", baseUrl, "-hls_time", "20", "-hls_list_size", "0", "-hls_segment_filename", tmpDir + "/output%d.ts", "-f", "hls", m3u8FilePath);
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();
        process.waitFor(30, TimeUnit.SECONDS);

        // 返回M3U8文件给前端
        InputStreamResource resource = new InputStreamResource(new FileInputStream(m3u8FilePath));
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(IOUtils.toByteArray(resource.getInputStream()));
    }

    /**
     * 视频切片获取
     *
     * @param resId
     * @param tsId
     * @param response
     */
    @RequestMapping(value = {"/video/{resId}/ts/{tsId}", "/video/undefined/video/{resId}/ts/{tsId}"}, method = RequestMethod.GET)
    public void fileOp(@PathVariable String resId,
                       @PathVariable String tsId,
                       HttpServletResponse response) {
        String tmpDir = config.getBase().getConfigFilePath() + "/tmp/" + resId + "/" + tsId;
        File file = new File(tmpDir);
        ExceptionCode.FILE_ACCESS_ERROR.throwOn(!file.exists(), "资源不存在");
        try (InputStream inputStream = new FileInputStream(file)) {
            response.reset();
            String contentType = FileHelper.getContentType(file);
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

}
