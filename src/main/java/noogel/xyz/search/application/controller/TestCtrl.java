package noogel.xyz.search.application.controller;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import noogel.xyz.search.infrastructure.config.ConfigProperties;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@Slf4j
public class TestCtrl {


    @Resource
    private ConfigProperties configProperties;

    @RequestMapping(value = "/test", method = RequestMethod.GET)
    public @ResponseBody String test() {
        return "{}";
    }
}
