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

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.alibaba.dubbo.common.utils.CollectionUtils;
import com.alibaba.dubboadmin.governance.service.OverrideService;
import com.alibaba.dubboadmin.governance.service.ProviderService;
import com.alibaba.dubboadmin.registry.common.domain.Provider;
import com.alibaba.dubboadmin.registry.common.domain.Weight;
import com.alibaba.dubboadmin.registry.common.util.OverrideUtils;
import com.alibaba.dubboadmin.web.mvc.BaseController;
import com.alibaba.dubboadmin.web.pulltool.Tool;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * ProvidersController.
 * URI: /services/$service/weights
 *
 */
@Controller
@RequestMapping("/governance/weights")
public class WeightsController extends BaseController {

    private static final Pattern IP_PATTERN = Pattern.compile("\\d{1,3}(\\.\\d{1,3}){3}$");
    private static final Pattern LOCAL_IP_PATTERN = Pattern.compile("127(\\.\\d{1,3}){3}$");
    private static final Pattern ALL_IP_PATTERN = Pattern.compile("0{1,3}(\\.0{1,3}){3}$");
    @Autowired
    private OverrideService overrideService;
    @Autowired
    private ProviderService providerService;

    @RequestMapping("")
    public String index(@RequestParam(required = false) String service,
                        @RequestParam(required = false) String address,
                        @RequestParam(required = false) String app,
                        @RequestParam(required = false) String keyWord,
                        HttpServletRequest request, HttpServletResponse response, Model model) {
        prepare(request, response, model, "index", "weights");
        service = StringUtils.trimToNull(service);
        address = Tool.getIP(address);
        List<Weight> weights;
        if (service != null && service.length() > 0) {
            weights = OverrideUtils.overridesToWeights(overrideService.findByService(service));
        } else if (address != null && address.length() > 0) {
            weights = OverrideUtils.overridesToWeights(overrideService.findByAddress(address));
        } else {
            weights = OverrideUtils.overridesToWeights(overrideService.findAll());
        }
        model.addAttribute("weights", weights);
        return "governance/screen/weights/index";
    }

    /**
     * load page for the adding
     *
     */
    @RequestMapping("/add")
    public String add(@RequestParam(required = false) String service,
                    HttpServletRequest request, HttpServletResponse response, Model model) {
        prepare(request, response, model, "add", "weights");
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
        if (model.addAttribute("input") != null) model.addAttribute("input", model.addAttribute("input"));
        return "governance/screen/weights/add";
    }

    /**
     * load page for the multi adding
     *
     * @param context
     */
    public void multiadd(Map<String, Object> context) {
        List<String> serviceList = Tool.sortSimpleName(providerService.findServices());
        context.put("serviceList", serviceList);
    }

    public boolean create(Map<String, Object> context) throws Exception {
        String addr = (String) context.get("address");
        String services = (String) context.get("multiservice");
        if (services == null || services.trim().length() == 0) {
            services = (String) context.get("service");
        }
        String weight = (String) context.get("weight");

        int w = Integer.parseInt(weight);

        Set<String> addresses = new HashSet<String>();
        BufferedReader reader = new BufferedReader(new StringReader(addr));
        while (true) {
            String line = reader.readLine();
            if (null == line)
                break;

            String[] split = line.split("[\\s,;]+");
            for (String s : split) {
                if (s.length() == 0)
                    continue;

                String ip = s;
                String port = null;
                if (s.indexOf(":") != -1) {
                    ip = s.substring(0, s.indexOf(":"));
                    port = s.substring(s.indexOf(":") + 1, s.length());
                    if (port.trim().length() == 0) port = null;
                }
                if (!IP_PATTERN.matcher(ip).matches()) {
                    context.put("message", "illegal IP: " + s);
                    return false;
                }
                if (LOCAL_IP_PATTERN.matcher(ip).matches() || ALL_IP_PATTERN.matcher(ip).matches()) {
                    context.put("message", "local IP or any host ip is illegal: " + s);
                    return false;
                }
                if (port != null) {
                    if (!NumberUtils.isDigits(port)) {
                        context.put("message", "illegal port: " + s);
                        return false;
                    }
                }
                addresses.add(s);
            }
        }

        Set<String> aimServices = new HashSet<String>();
        reader = new BufferedReader(new StringReader(services));
        while (true) {
            String line = reader.readLine();
            if (null == line)
                break;

            String[] split = line.split("[\\s,;]+");
            for (String s : split) {
                if (s.length() == 0)
                    continue;
                if (!super.currentUser.hasServicePrivilege(s)) {
                    context.put("message", getMessage("HaveNoServicePrivilege", s));
                    return false;
                }
                aimServices.add(s);
            }
        }

        for (String aimService : aimServices) {
            for (String a : addresses) {
                Weight wt = new Weight();
                wt.setUsername((String) context.get("operator"));
                wt.setAddress(Tool.getIP(a));
                wt.setService(aimService);
                wt.setWeight(w);
                overrideService.saveOverride(OverrideUtils.weightToOverride(wt));
            }
        }
        return true;
    }

    public void edit(Long id, Map<String, Object> context) {
        //add(context);
        show(id, context);
        context.put("service", overrideService.findById(id).getService());
    }

    public void sameSeviceEdit(Long id, Map<String, Object> context) {
        //add(context);
        show(id, context);
    }

    /**
     * load weight for editing
     *
     * @param id
     * @param context
     */
    public void show(Long id, Map<String, Object> context) {
        Weight weight = OverrideUtils.overrideToWeight(overrideService.findById(id));
        context.put("weight", weight);
    }

    public boolean update(Weight weight, Map<String, Object> context) {
        if (!super.currentUser.hasServicePrivilege(weight.getService())) {
            context.put("message", getMessage("HaveNoServicePrivilege", weight.getService()));
            return false;
        }
        weight.setAddress(Tool.getIP(weight.getAddress()));
        overrideService.updateOverride(OverrideUtils.weightToOverride(weight));
        return true;
    }

    /**
     * delete
     *
     * @param ids
     * @return
     */
    public boolean delete(Long[] ids, Map<String, Object> context) {
        for (Long id : ids) {
            Weight w = OverrideUtils.overrideToWeight(overrideService.findById(id));
            if (!super.currentUser.hasServicePrivilege(w.getService())) {
                context.put("message", getMessage("HaveNoServicePrivilege", w.getService()));
                return false;
            }
        }

        for (Long id : ids) {
            overrideService.deleteOverride(id);
        }
        return true;
    }

}
