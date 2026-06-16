package com.sys.pro.generate.strategy;

import com.sys.pro.generate.pojo.BaseInfo;

/**
 * ComponentGenerationStrategy 承载ComponentGenerationStrategy模块的支撑代码。
 */
public interface ComponentGenerationStrategy {
    /**
     * 完成ComponentGenerationStrategy中的 generateComponent 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param baseInfo baseInfo 字段，来源于当前接口入参或内部调用上下文。
     * @return ComponentGenerationStrategy处理后的文本结果。
     */



    String generateComponent(BaseInfo baseInfo);

}

