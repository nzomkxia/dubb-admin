package com.alibaba.dubboadmin.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;

/**
 * @author zmx ON 2018/5/25
 */

@Configuration
@ImportResource({"classpath*:dubbo-admin.xml"})
public class XmlConfiguration {

}
