package com.zzy.generate.enums;

/**
 * 模板名称枚举类
 */
public enum TemplateEnum {

    ENTITY("entity.java.ftl"),
    ENTITY_VO("entityVo.java.ftl"),
    MAPPER("mapper.java.ftl"),
    MAPPER_XML("mapper.xml.ftl"),
    SERVICE("service.java.ftl"),
    SERVICE_IMPL("serviceImpl.java.ftl"),
    CONTROLLER("controller.java.ftl");

    public final String templateName;

    TemplateEnum(String templateName) {
        this.templateName = templateName;
    }
}
