package com.alibaba.dubboadmin.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;


@Configuration
@ImportResource({"classpath*:dubbo-admin.xml"})
public class XmlConfiguration {

}
