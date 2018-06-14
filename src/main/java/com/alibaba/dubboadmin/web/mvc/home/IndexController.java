package com.alibaba.dubboadmin.web.mvc.home;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubboadmin.governance.service.ConsumerService;
import com.alibaba.dubboadmin.governance.service.ProviderService;
import com.alibaba.dubboadmin.registry.common.domain.Consumer;
import com.alibaba.dubboadmin.registry.common.domain.Provider;
import com.alibaba.dubboadmin.web.pulltool.RootContextPath;
import com.alibaba.dubboadmin.web.pulltool.Tool;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class IndexController {

    private static final Logger logger = LoggerFactory.getLogger(IndexController.class);
    @Autowired
    private ProviderService providerService;

    @Autowired
    private ConsumerService consumerService;

    @RequestMapping("/")
    public String indexRequest(HttpServletRequest request, Model model) {
        Set<String> applications = new HashSet<String>();
        Set<String> services = new HashSet<String>();
        List<Provider> pList = new ArrayList<Provider>();
        try {
            pList = providerService.findAll();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        for (Provider p : pList) {
            applications.add(p.getApplication());
            services.add(p.getService());
        }
        List<Consumer> cList = new ArrayList<Consumer>();
        try {
            cList = consumerService.findAll();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        for (Consumer c : cList) {
            applications.add(c.getApplication());
            services.add(c.getService());
        }
        model.addAttribute("rootContextPath", new RootContextPath(request.getContextPath()));
        model.addAttribute("services", services.size());
        model.addAttribute("providers", pList.size());
        model.addAttribute("consumers", cList.size());
        model.addAttribute("applications", applications.size());
        model.addAttribute("tool", new Tool());
        return "home/screen/index";

    }

}
