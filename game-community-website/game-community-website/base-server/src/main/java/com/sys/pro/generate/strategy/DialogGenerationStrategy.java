package com.sys.pro.generate.strategy;

import com.sys.pro.generate.pojo.BaseInfo;
import com.sys.pro.generate.pojo.DialogInfo;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
/**
 * 弹窗表单生成策略，根据字段注解拼装新增和编辑弹窗配置。
 */
@Slf4j

@Component

@RequiredArgsConstructor
public class DialogGenerationStrategy implements ComponentGenerationStrategy {



    private final Configuration freemarkerConfig;



    /**
     * 完成DialogGenerationStrategy中的 generateComponent 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param baseInfo baseInfo 字段，来源于当前接口入参或内部调用上下文。
     * @return DialogGenerationStrategy处理后的文本结果。
     */
    @Override
    public String generateComponent(BaseInfo baseInfo) {
        try {
            log.info("生成对话框组件，参数:{}", baseInfo);

            DialogInfo dialogInfo = (DialogInfo) baseInfo;
            // 将数据填充到模板
            Map<String, Object> dataModel = new HashMap<>();
            dataModel.put("fields", dialogInfo.getFields());
            dataModel.put("title", dialogInfo.getTitle());
            dataModel.put("model", dialogInfo.getModel());

            return processTemplate(dataModel);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

    /**
     * 完成DialogGenerationStrategy中的 processTemplate 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param dataModel dataModel 字段，来源于当前接口入参或内部调用上下文。
     * @return DialogGenerationStrategy处理后的文本结果。
     */
    private String processTemplate(Map<String, Object> dataModel) {
        try (StringWriter stringWriter = new StringWriter()) {
            Template template = freemarkerConfig.getTemplate("el-dialog.ftl");
            template.process(dataModel, stringWriter);
            return stringWriter.toString();
        } catch (IOException | TemplateException e) {
            /**
             * 完成DialogGenerationStrategy中的 RuntimeException 步骤，保证该环节的数据和状态可以继续向下流转。
             * @param e e 字段，来源于当前接口入参或内部调用上下文。
             * @return DialogGenerationStrategy在该步骤产出的业务结果。
             */
            throw new RuntimeException("Failed to process template", e);
        }
    }
}
