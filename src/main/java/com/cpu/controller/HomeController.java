package com.cpu.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {
    @GetMapping({"/"})
    public String index() {
        return "index";  // templates/index.html을 렌더링
    }
}