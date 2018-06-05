package com.alibaba.dubboadmin.web.mvc;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.common.utils.CompatibleTypeUtils;
import com.alibaba.dubbo.common.utils.StringUtils;
import com.alibaba.dubboadmin.governance.biz.common.i18n.MessageResourceService;
import com.alibaba.dubboadmin.governance.util.WebConstants;
import com.alibaba.dubboadmin.registry.common.domain.User;
import com.alibaba.dubboadmin.web.pulltool.RootContextPath;
import com.alibaba.dubboadmin.web.pulltool.Tool;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;

/**
 * @author zmx ON 2018/5/28
 */
public class BaseController {
    protected static final Logger logger = LoggerFactory.getLogger(BaseController.class);

    protected static final Pattern SPACE_SPLIT_PATTERN = Pattern.compile("\\s+");
    //FIXME, to extract these auxiliary methods
    protected String role = null;
    protected String operator = null;
    protected User currentUser = null;
    protected String operatorAddress = null;
    protected String currentRegistry = null;
    @Autowired
    private MessageResourceService messageResourceService;

    @Autowired
    protected Tool tool;

    private static boolean isPrimitive(Class<?> cls) {
        return cls.isPrimitive() || cls == Boolean.class || cls == Byte.class
            || cls == Character.class || cls == Short.class || cls == Integer.class
            || cls == Long.class || cls == Float.class || cls == Double.class
            || cls == String.class;
    }

    private static Object convertPrimitive(Class<?> cls, String value) {
        if (cls == boolean.class || cls == Boolean.class) {
            return value == null || value.length() == 0 ? false : Boolean.valueOf(value);
        } else if (cls == byte.class || cls == Byte.class) {
            return value == null || value.length() == 0 ? 0 : Byte.valueOf(value);
        } else if (cls == char.class || cls == Character.class) {
            return value == null || value.length() == 0 ? '\0' : value.charAt(0);
        } else if (cls == short.class || cls == Short.class) {
            return value == null || value.length() == 0 ? 0 : Short.valueOf(value);
        } else if (cls == int.class || cls == Integer.class) {
            return value == null || value.length() == 0 ? 0 : Integer.valueOf(value);
        } else if (cls == long.class || cls == Long.class) {
            return value == null || value.length() == 0 ? 0 : Long.valueOf(value);
        } else if (cls == float.class || cls == Float.class) {
            return value == null || value.length() == 0 ? 0 : Float.valueOf(value);
        } else if (cls == double.class || cls == Double.class) {
            return value == null || value.length() == 0 ? 0 : Double.valueOf(value);
        }
        return value;
    }


    public void prepare(HttpServletRequest request, HttpServletResponse response, Model model,
                        String methodName, String type) {
        if (request.getSession().getAttribute(WebConstants.CURRENT_USER_KEY) != null) {
            User user = (User) request.getSession().getAttribute(WebConstants.CURRENT_USER_KEY);
            currentUser = user;
            operator = user.getUsername();
            role = user.getRole();
            request.getSession().setAttribute(WebConstants.CURRENT_USER_KEY, user);
        }
        operatorAddress = request.getRemoteHost();
        request.getMethod();
        model.addAttribute("operator", operator);
        model.addAttribute("operatorAddress", operatorAddress);

        model.addAttribute("currentRegistry", currentRegistry);
        model.addAttribute("rootContextPath", new RootContextPath(request.getContextPath()));
        model.addAttribute("tool", tool);
        model.addAttribute("_method", methodName);
        model.addAttribute("_type", type);

    }

    public String getMessage(String key, Object... args) {
        return messageResourceService.getMessage(key, args);
    }

    private String getDefaultRedirect(Map<String, Object> context, String operate) {
        String defaultRedirect = (String) context.get("defaultRedirect");
        return defaultRedirect;
    }

}
