package com.sys.pro.common;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.validation.constraints.NotNull;

/**
 * SingleIdParam 提供SingleId通用能力，统一项目内重复使用的基础模型或拦截逻辑。
 */
@Getter
@Setter
@ToString(callSuper = true)
public class SingleIdParam {

    @NotNull
    private Long id;
}

