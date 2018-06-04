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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.alibaba.dubbo.common.utils.StringUtils;
import com.alibaba.dubboadmin.governance.service.OverrideService;
import com.alibaba.dubboadmin.governance.service.ProviderService;
import com.alibaba.dubboadmin.registry.common.domain.Override;
import com.alibaba.dubboadmin.registry.common.domain.Provider;
import com.alibaba.dubboadmin.registry.common.route.OverrideUtils;
import com.alibaba.dubboadmin.web.mvc.BaseController;
import com.alibaba.dubboadmin.web.pulltool.Tool;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * <p>ProvidersController.</p>
 * URI: <br>
 * GET /providers, show all providers<br>
 * GET /providers/add, show web form for add a static provider<br>
 * POST /provider/create, create a static provider, save form<br>
 * GET /providers/$id, show provider details<br>
 * GET /providers/$id/edit, web form for edit provider<br>
 * POST /providers/$id, update provider, save form<br>
 * GET /providers/$id/delete, delete a provider<br>
 * GET /providers/$id/tostatic, transfer to static<br>
 * GET /providers/$id/todynamic, transfer to dynamic<br>
 * GET /providers/$id/enable, enable a provider<br>
 * GET /providers/$id/disable, disable a provider<br>
 * GET /providers/$id/reconnect, reconnect<br>
 * GET /providers/$id/recover, recover<br>
 * <br>
 * GET /services/$service/providers, show all provider of a specific service<br>
 * GET /services/$service/providers/add, show web form for add a static provider<br>
 * POST /services/$service/providers, save a static provider<br>
 * GET /services/$service/providers/$id, show provider details<br>
 * GET /services/$service/providers/$id/edit, show web form for edit provider<br>
 * POST /services/$service/providers/$id, save changes of provider<br>
 * GET /services/$service/providers/$id/delete, delete provider<br>
 * GET /services/$service/providers/$id/tostatic, transfer to static<br>
 * GET /services/$service/providers/$id/todynamic, transfer to dynamic<br>
 * GET /services/$service/providers/$id/enable, enable<br>
 * GET /services/$service/providers/$id/disable, diable<br>
 * GET /services/$service/providers/$id/reconnect, reconnect<br>
 * GET /services/$service/providers/$id/recover, recover<br>
 *
 */
@Controller
@RequestMapping("/governance/providers")
public class ProvidersController extends BaseController {

    @Autowired
    private ProviderService providerService;

    @Autowired
    private OverrideService overrideService;

    @RequestMapping("")
    public String index(HttpServletRequest request, HttpServletResponse response, Model model,
                      @RequestParam(required = false) String service,
                      @RequestParam(required = false) String application,
                      @RequestParam(required = false) String address) {

        prepare(request, response, model, "index", "providers");

        String value = "";
        String separators = "....";

        List<Provider> providers = null;

        // service
        if (service != null && service.length() > 0) {
            providers = providerService.findByService(service);

            value = service + separators + request.getRequestURI();
        }
        // address
        else if (address != null && address.length() > 0) {
            providers = providerService.findByAddress(address);

            value = address + separators + request.getRequestURI();
        }
        // application
        else if (application != null && application.length() > 0) {
            providers = providerService.findByApplication(application);

            value = application + separators + request.getRequestURI();
        }
        // all
        else {
            providers = providerService.findAll();
        }

        model.addAttribute("providers", providers);
        model.addAttribute("serviceAppMap", getServiceAppMap(providers));
        model.addAttribute("tool", new Tool());

        // record search history to cookies
        setSearchHistroy(value, request, response);
        return "governance/screen/providers/index";
    }

    /**
     *
     * Calculate the application list corresponding to each service, to facilitate the "repeat" prompt on service page
     * @param providers app services
     */
    private Map<String, Set<String>> getServiceAppMap(List<Provider> providers) {
        Map<String, Set<String>> serviceAppMap = new HashMap<String, Set<String>>();
        if (providers != null && providers.size() > 0) {
            for (Provider provider : providers) {
                Set<String> appSet;
                String service = provider.getService();
                if (serviceAppMap.get(service) == null) {
                    appSet = new HashSet<String>();
                } else {
                    appSet = serviceAppMap.get(service);
                }
                appSet.add(provider.getApplication());
                serviceAppMap.put(service, appSet);
            }
        }
        return serviceAppMap;
    }

    /**
     * Record search history to cookies, steps:
     * Check whether the added record exists in the cookie, and if so, update the list order; if it does not exist, insert it to the front
     *
     * @param value
     */
    private void setSearchHistroy(String value, HttpServletRequest request, HttpServletResponse response) {
        // Analyze existing cookies
        String separatorsB = "\\.\\.\\.\\.\\.\\.";
        String newCookiev = value;
        Cookie[] cookies = request.getCookies();
        for (Cookie c : cookies) {
            if (c.getName().equals("HISTORY")) {
                String cookiev = c.getValue();
                String[] values = cookiev.split(separatorsB);
                int count = 1;
                for (String v : values) {
                    if (count <= 10) {
                        if (!value.equals(v)) {
                            newCookiev = newCookiev + separatorsB + v;
                        }
                    }
                    count++;
                }
                break;
            }
        }

        Cookie _cookie = new Cookie("HISTORY", newCookiev);
        _cookie.setMaxAge(60 * 60 * 24 * 7); // Set the cookie's lifetime to 30 minutes
        _cookie.setPath("/");
        response.addCookie(_cookie); // Write to client hard disk
    }

    @RequestMapping("/show")
    public String show(@RequestParam Long id, Model model) {
        Provider provider = providerService.findProvider(id);
        if (provider != null && provider.isDynamic()) {
            List<Override> overrides = overrideService.findByServiceAndAddress(provider.getService(), provider.getAddress());
            OverrideUtils.setProviderOverrides(provider, overrides);
        }
        model.addAttribute("provider", provider);
        return "governance/screen/show";
    }

    /**
     * Load new service page, get all the service name
     *
     */
    @RequestMapping("/add")
    public String add(@RequestParam(required = false) Long id, @RequestParam(required = false) String service,
                      HttpServletRequest request, HttpServletResponse response, Model model) {
        prepare(request, response, model, "add", "providers");
        if (service == null) {
            List<String> serviceList = Tool.sortSimpleName(new ArrayList<String>(providerService.findServices()));
            model.addAttribute("serviceList", serviceList);
        }
        if (id != null) {
            Provider p = providerService.findProvider(id);
            if (p != null) {
                model.addAttribute("provider", p);
                String parameters = p.getParameters();
                if (parameters != null && parameters.length() > 0) {
                    Map<String, String> map = StringUtils.parseQueryString(parameters);
                    map.put("timestamp", String.valueOf(System.currentTimeMillis()));
                    map.remove("pid");
                    p.setParameters(StringUtils.toQueryString(map));
                }
            }
        }
        return "governance/screen/providers/add";
    }

    @RequestMapping("/edit")
    public void edit(@RequestParam Long id, Model model) {
        show(id, model);
    }

    @RequestMapping(value =  "/create", method = RequestMethod.POST)  //post
    public boolean create(@ModelAttribute Provider provider, Model model) {
        String service = provider.getService();
        if (!super.currentUser.hasServicePrivilege(service)) {
            model.addAttribute("message", getMessage("HaveNoServicePrivilege", service));
            return false;
        }
        if (provider.getParameters() == null) {
            String url = provider.getUrl();
            if (url != null) {
                int i = url.indexOf('?');
                if (i > 0) {
                    provider.setUrl(url.substring(0, i));
                    provider.setParameters(url.substring(i + 1));
                }
            }
        }
        provider.setDynamic(false); // Provider add through web page must be static
        providerService.create(provider);
        return true;
    }

    @RequestMapping(value = "/update", method = RequestMethod.POST) //post
    public boolean update(@ModelAttribute Provider newProvider, Model model) {
        Long id = newProvider.getId();
        String parameters = newProvider.getParameters();
        Provider provider = providerService.findProvider(id);
        if (provider == null) {
            model.addAttribute("message", getMessage("NoSuchOperationData", id));
            return false;
        }
        String service = provider.getService();
        if (!super.currentUser.hasServicePrivilege(service)) {
            model.addAttribute("message", getMessage("HaveNoServicePrivilege", service));
            return false;
        }
        Map<String, String> oldMap = StringUtils.parseQueryString(provider.getParameters());
        Map<String, String> newMap = StringUtils.parseQueryString(parameters);
        for (Map.Entry<String, String> entry : oldMap.entrySet()) {
            if (entry.getValue().equals(newMap.get(entry.getKey()))) {
                newMap.remove(entry.getKey());
            }
        }
        if (provider.isDynamic()) {
            String address = provider.getAddress();
            List<Override> overrides = overrideService.findByServiceAndAddress(provider.getService(), provider.getAddress());
            OverrideUtils.setProviderOverrides(provider, overrides);
            Override override = provider.getOverride();
            if (override != null) {
                if (newMap.size() > 0) {
                    override.setParams(StringUtils.toQueryString(newMap));
                    override.setEnabled(true);
                    override.setOperator(operator);
                    override.setOperatorAddress(operatorAddress);
                    overrideService.updateOverride(override);
                } else {
                    overrideService.deleteOverride(override.getId());
                }
            } else {
                override = new Override();
                override.setService(service);
                override.setAddress(address);
                override.setParams(StringUtils.toQueryString(newMap));
                override.setEnabled(true);
                override.setOperator(operator);
                override.setOperatorAddress(operatorAddress);
                overrideService.saveOverride(override);
            }
        } else {
            provider.setParameters(parameters);
            providerService.updateProvider(provider);
        }
        return true;
    }

    @RequestMapping("/delete")
    public boolean delete(@RequestParam Long[] ids, Model model) {
        for (Long id : ids) {
            Provider provider = providerService.findProvider(id);
            if (provider == null) {
                model.addAttribute("message", getMessage("NoSuchOperationData", id));
                return false;
            } else if (provider.isDynamic()) {
                model.addAttribute("message", getMessage("CanNotDeleteDynamicData", id));
                return false;
            } else if (!super.currentUser.hasServicePrivilege(provider.getService())) {
                model.addAttribute("message", getMessage("HaveNoServicePrivilege", provider.getService()));
                return false;
            }
        }
        for (Long id : ids) {
            providerService.deleteStaticProvider(id);
        }
        return true;
    }

    @RequestMapping("/enable")
    public boolean enable(@RequestParam Long[] ids, Model model) {
        Map<Long, Provider> id2Provider = new HashMap<Long, Provider>();
        for (Long id : ids) {
            Provider provider = providerService.findProvider(id);
            if (provider == null) {
                model.addAttribute("message", getMessage("NoSuchOperationData", id));
                return false;
            } else if (!super.currentUser.hasServicePrivilege(provider.getService())) {
                model.addAttribute("message", getMessage("HaveNoServicePrivilege", provider.getService()));
                return false;
            }
            id2Provider.put(id, provider);
        }
        for (Long id : ids) {
            providerService.enableProvider(id);
        }
        return true;
    }

    @RequestMapping("/disable")
    public boolean disable(@RequestParam Long[] ids, Model model) {
        for (Long id : ids) {
            Provider provider = providerService.findProvider(id);
            if (provider == null) {
                model.addAttribute("message", getMessage("NoSuchOperationData", id));
                return false;
            } else if (!super.currentUser.hasServicePrivilege(provider.getService())) {
                model.addAttribute("message", getMessage("HaveNoServicePrivilege", provider.getService()));
                return false;
            }
        }
        for (Long id : ids) {
            providerService.disableProvider(id);
        }
        return true;
    }

    @RequestMapping("/doubling")
    public boolean doubling(@RequestParam Long[] ids, Model model) {
        for (Long id : ids) {
            Provider provider = providerService.findProvider(id);
            if (provider == null) {
                model.addAttribute("message", getMessage("NoSuchOperationData", id));
                return false;
            } else if (!super.currentUser.hasServicePrivilege(provider.getService())) {
                model.addAttribute("message", getMessage("HaveNoServicePrivilege", provider.getService()));
                return false;
            }
        }
        for (Long id : ids) {
            providerService.doublingProvider(id);
        }
        return true;
    }

    @RequestMapping("/halving")
    public boolean halving(@RequestParam Long[] ids, Model model) {
        for (Long id : ids) {
            Provider provider = providerService.findProvider(id);
            if (provider == null) {
                model.addAttribute("message", getMessage("NoSuchOperationData", id));
                return false;
            } else if (!super.currentUser.hasServicePrivilege(provider.getService())) {
                model.addAttribute("message", getMessage("HaveNoServicePrivilege", provider.getService()));
                return false;
            }
        }
        for (Long id : ids) {
            providerService.halvingProvider(id);
        }
        return true;
    }

}
