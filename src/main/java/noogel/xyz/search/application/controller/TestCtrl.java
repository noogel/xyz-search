package noogel.xyz.search.application.controller;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import noogel.xyz.search.service.extension.impl.ImageExtensionPointServiceImpl;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@Slf4j
public class TestCtrl {

    @Resource
    private ImageExtensionPointServiceImpl imageExtensionPointService;

    @RequestMapping(value = "/test", method = RequestMethod.GET)
    public @ResponseBody String test() {
        return "{}";
    }
}
