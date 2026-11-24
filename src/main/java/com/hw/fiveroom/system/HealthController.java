package com.hw.fiveroom.system;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
//处理HTTP请求的控制器
@RequestMapping("/api")
//设置统一的接口路径前缀
public class HealthController {
    @GetMapping("/health")
    public String health(){
        return "halo";
    }
}
