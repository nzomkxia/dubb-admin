package com.alibaba.dubboadmin.web.mvc.governance;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * @author zmx ON 2018/6/6
 */
@Controller
public class TestController {

    @RequestMapping("/test/{type}/aaa")
    public String testController(@PathVariable("type") String type, HttpServletRequest request,
                                 HttpServletResponse response, Model model) {
        System.out.println("test controller");
        return "";
    }
}
