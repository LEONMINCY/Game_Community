package ${packageName};

import com.baomidou.mybatisplus.annotation.*;
<#if swagger2>
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
</#if>
<#if entityLombokModel>
import lombok.Data;
import lombok.EqualsAndHashCode;
</#if>
import java.io.Serializable;
import com.sys.${author}.common.PageParam;
<#list packagePathList as package>
import ${package};
</#list>

/**
 * <p>
 * ${table.comment!}
 * </p>
 *
 * @author ${author}
 */
<#if entityLombokModel>
@Data
    <#if superEntityClass??>
@EqualsAndHashCode(callSuper = true)
    <#else>
@EqualsAndHashCode(callSuper = false)
    </#if>
</#if>
<#if table.convert>
@TableName("${table.name}")
</#if>
<#if swagger2>
@ApiModel(value="${entity}对象", description="${table.comment!}")
</#if>
public class ${entity}Vo extends PageParam implements Serializable {

<#if entitySerialVersionUID>
    private static final long serialVersionUID = 1L;
</#if>
<#-- ----------  BEGIN 字段循环遍历  ---------->
<#list table.fields as field>

    <#if field.fieldComment!?length gt 0>
        <#if swagger2>
    @ApiModelProperty(value = "${field.fieldComment}")
        <#else>
    /**
     * ${field.fieldComment}
     */
        </#if>
    </#if>
    <#if field.fieldType == 'LocalDateTime' || field.fieldType == 'LocalDate'>
    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss", timezone="GMT+8")
    </#if>
    private ${field.fieldType} ${field.fieldName};
</#list>
<#------------  END 字段循环遍历  ---------->

}
