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
        model.addAttribute("tool", new Tool());
        model.addAttribute("_method", methodName);
        model.addAttribute("_type", type);

        //String httpMethod = (String) context.get("request.method");
        //String method = (String) context.get("_method");
        //String contextPath = (String) context.get("request.contextPath");
        //context.put("rootContextPath", new RootContextPath(contextPath));
        //
        //// Analyze Method
        //if (method == null || method.length() == 0) {
        //    String id = (String) context.get("id");
        //    if (id == null || id.length() == 0) {
        //        method = "index";
        //    } else {
        //        method = "show";
        //    }
        //}
        //if ("index".equals(method)) {
        //    if ("post".equalsIgnoreCase(httpMethod)) {
        //        method = "create";
        //    }
        //} else if ("show".equals(method)) {
        //    if ("put".equalsIgnoreCase(httpMethod) || "post".equalsIgnoreCase(httpMethod)) { // Instead of submitting a PUT request with a form, use POST instead
        //        method = "update";
        //    } else if ("delete".equalsIgnoreCase(httpMethod)) { // Instead of submitting a PUT request with a form, use POST instead
        //        method = "delete";
        //    }
        //}
        //context.put("_method", method);
        //
        //try {
        //    Method m = null;
        //    try {
        //        m = getClass().getMethod(method, new Class<?>[]{Map.class});
        //    } catch (NoSuchMethodException e) {
        //        for (Method mtd : getClass().getMethods()) {
        //            if (Modifier.isPublic(mtd.getModifiers())
        //                && mtd.getName().equals(method)) {
        //                m = mtd;
        //                break;
        //            }
        //        }
        //        if (m == null) {
        //            throw e;
        //        }
        //    }
        //    if (m.getParameterTypes().length > 2) {
        //        throw new IllegalStateException("Unsupport restful method " + m);
        //    } else if (m.getParameterTypes().length == 2
        //        && (m.getParameterTypes()[0].equals(Map.class)
        //        || !m.getParameterTypes()[1].equals(Map.class))) {
        //        throw new IllegalStateException("Unsupport restful method " + m);
        //    }
        //    Object r;
        //    if (m.getParameterTypes().length == 0) {
        //        r = m.invoke(this, new Object[0]);
        //    } else {
        //        Object value;
        //        Class<?> t = m.getParameterTypes()[0];
        //        if (Map.class.equals(t)) {
        //            value = context;
        //        } else if (isPrimitive(t)) {
        //            String id = (String) context.get("id");
        //            value = convertPrimitive(t, id);
        //        } else if (t.isArray() && isPrimitive(t.getComponentType())) {
        //            String id = (String) context.get("id");
        //            String[] ids = id == null ? new String[0] : id.split("[.+]+");
        //            value = Array.newInstance(t.getComponentType(), ids.length);
        //            for (int i = 0; i < ids.length; i++) {
        //                Array.set(value, i, convertPrimitive(t.getComponentType(), ids[i]));
        //            }
        //        } else {
        //            value = t.newInstance();
        //            for (Method mtd : t.getMethods()) {
        //                if (Modifier.isPublic(mtd.getModifiers())
        //                    && mtd.getName().startsWith("set")
        //                    && mtd.getParameterTypes().length == 1) {
        //                    String p = mtd.getName().substring(3, 4).toLowerCase() + mtd.getName().substring(4);
        //                    Object v = context.get(p);
        //                    if (v == null) {
        //                        if ("operator".equals(p)) {
        //                            v = operator;
        //                        } else if ("operatorAddress".equals(p)) {
        //                            v = (String) context.get("request.remoteHost");
        //                        }
        //                    }
        //                    if (v != null) {
        //                        try {
        //                            mtd.invoke(value, new Object[]{CompatibleTypeUtils.compatibleTypeConvert(v, mtd.getParameterTypes()[0])});
        //                        } catch (Throwable e) {
        //                            logger.warn(e.getMessage(), e);
        //                        }
        //                    }
        //                }
        //            }
        //        }
        //        if (m.getParameterTypes().length == 1) {
        //            r = m.invoke(this, new Object[]{value});
        //        } else {
        //            r = m.invoke(this, new Object[]{value, context});
        //        }
        //    }
        //    if (m.getReturnType() == boolean.class || m.getReturnType() == Boolean.class) {
        //        context.put("rundata.layout", "redirect");
        //        context.put("rundata.target", "redirect");
        //        context.put("success", r == null || ((Boolean) r).booleanValue());
        //        if (context.get("redirect") == null) {
        //            context.put("redirect", getDefaultRedirect(context, method));
        //        }
        //    } else if (m.getReturnType() == String.class) {
        //        String redirect = (String) r;
        //        if (redirect == null) {
        //            redirect = getDefaultRedirect(context, method);
        //        }
        //
        //        if (context.get("chain") != null) {
        //            context.put("rundata.layout", "home");
        //            context.put("rundata.target", "home");
        //        } else {
        //            context.put("rundata.redirect", redirect);
        //        }
        //    } else {
        //        context.put("rundata.layout", method);
        //        context.put("rundata.target", context.get("rundata.target") + "/" + method);
        //    }
        //} catch (Throwable e) {
        //    if (e instanceof InvocationTargetException) {
        //        throw ((InvocationTargetException) e).getTargetException();
        //    }
        //    //            if (e instanceof InvocationTargetException) {
        //    //                e = ((InvocationTargetException) e).getTargetException();
        //    //            }
        //    //            logger.warn(e.getMessage(), e);
        //    //            context.put("rundata.layout", "redirect");
        //    //            context.put("rundata.target", "redirect");
        //    //            context.put("success", false);
        //    //            context.put("exception", e);
        //    //            context.put("redirect", getDefaultRedirect(context, method));
        //}
    }

    public String getMessage(String key, Object... args) {
        return messageResourceService.getMessage(key, args);
    }

    private String getDefaultRedirect(Map<String, Object> context, String operate) {
        String defaultRedirect = (String) context.get("defaultRedirect");
        return defaultRedirect;
    }

}
