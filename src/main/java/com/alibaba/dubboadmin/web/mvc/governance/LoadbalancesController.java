/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.dubboadmin.web.mvc.governance;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.alibaba.dubbo.common.utils.CollectionUtils;
import com.alibaba.dubboadmin.governance.service.OverrideService;
import com.alibaba.dubboadmin.governance.service.ProviderService;
import com.alibaba.dubboadmin.registry.common.domain.LoadBalance;
import com.alibaba.dubboadmin.registry.common.domain.Provider;
import com.alibaba.dubboadmin.registry.common.util.OverrideUtils;
import com.alibaba.dubboadmin.web.mvc.BaseController;
import com.alibaba.dubboadmin.web.pulltool.Tool;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * ProvidersController.
 * URI: /services/$service/loadbalances
 *
 */
@Controller
@RequestMapping("/governance/loadbalances")
public class LoadbalancesController extends BaseController {

    @Autowired
    private OverrideService overrideService;

    @Autowired
    private ProviderService providerService;

    @RequestMapping("")
    public String index(@RequestParam(required = false) String service,@RequestParam(required = false) String address,
                        @RequestParam(required = false) String app, @RequestParam(required = false) String keyWord,
                        HttpServletRequest request, HttpServletResponse response, Model model) {
        prepare(request, response, model, "index", "loadbalances");
        service = StringUtils.trimToNull(service);

        List<LoadBalance> loadbalances;
        if (service != null && service.length() > 0) {
            loadbalances = OverrideUtils.overridesToLoadBalances(overrideService.findByService(service));
        } else {
            loadbalances = OverrideUtils.overridesToLoadBalances(overrideService.findAll());
        }
        model.addAttribute("loadbalances", loadbalances);
        return "governance/screen/loadbalances/index";
    }

    @RequestMapping("/show")
    public void show(Long id, Map<String, Object> context) {
        LoadBalance loadbalance = OverrideUtils.overrideToLoadBalance(overrideService.findById(id));
        context.put("loadbalance", loadbalance);
    }

    @RequestMapping("/add")
    public String add(@RequestParam(required = false) String service,
                      @RequestParam(required = false) String input,
                      HttpServletRequest request, HttpServletResponse response, Model model) {
        prepare(request, response, model, "add", "loadbalances");
        if (service != null && service.length() > 0 && !service.contains("*")) {
            List<Provider> providerList = providerService.findByService(service);
            List<String> addressList = new ArrayList<String>();
            for (Provider provider : providerList) {
                addressList.add(provider.getUrl().split("://")[1].split("/")[0]);
            }
            model.addAttribute("addressList", addressList);
            model.addAttribute("service", service);
            model.addAttribute("methods", CollectionUtils.sort(providerService.findMethodsByService(service)));
        } else {
            List<String> serviceList = Tool.sortSimpleName(providerService.findServices());
            model.addAttribute("serviceList", serviceList);
        }
        if (input != null) model.addAttribute("input", input);
        return "governance/screen/loadbalances/add";
    }

    @RequestMapping("/edit")
    public void edit(Long id, Map<String, Object> context) {
        //add(context);
        show(id, context);
    }

    @RequestMapping("/create")
    public boolean create(LoadBalance loadBalance, Map<String, Object> context) {
        if (!super.currentUser.hasServicePrivilege(loadBalance.getService())) {
            context.put("message", getMessage("HaveNoServicePrivilege", loadBalance.getService()));
            return false;
        }

        loadBalance.setUsername((String) context.get("operator"));
        overrideService.saveOverride(OverrideUtils.loadBalanceToOverride(loadBalance));
        return true;
    }


    @RequestMapping("/update")
    public boolean update(LoadBalance loadBalance, Map<String, Object> context) {
        if (!super.currentUser.hasServicePrivilege(loadBalance.getService())) {
            context.put("message", getMessage("HaveNoServicePrivilege", loadBalance.getService()));
            return false;
        }
        overrideService.updateOverride(OverrideUtils.loadBalanceToOverride(loadBalance));
        return true;
    }

    /**
     *
     * @param ids
     * @return
     */
    @RequestMapping("/delete")
    public boolean delete(Long[] ids, Map<String, Object> context) {
        for (Long id : ids) {
            LoadBalance lb = OverrideUtils.overrideToLoadBalance(overrideService.findById(id));
            if (!super.currentUser.hasServicePrivilege(lb.getService())) {
                context.put("message", getMessage("HaveNoServicePrivilege", lb.getService()));
                return false;
            }
        }

        for (Long id : ids) {
            overrideService.deleteOverride(id);
        }
        return true;
    }

}
