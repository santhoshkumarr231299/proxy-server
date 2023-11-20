package com.proxy.recon.controller;

import com.proxy.recon.service.ProxyService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@ResponseBody
public class RequestHandler {
    @Autowired
    private ProxyService proxyService;
    @RequestMapping("/**")
    public ResponseEntity proxyControl(HttpServletRequest request) {
        return proxyService.proxyGateWay(request);
    }
}
