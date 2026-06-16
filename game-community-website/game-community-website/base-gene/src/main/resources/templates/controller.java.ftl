package ${packageName};

import com.sys.${author}.service.${table.tableNameUP}Service;
import com.sys.${author}.pojo.${table.tableNameUP};
import io.swagger.annotations.ApiOperation;
import com.sys.${author}.common.CommonResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
<#if restControllerStyle>
import org.springframework.web.bind.annotation.*;
<#else>
import org.springframework.stereotype.Controller;
</#if>
<#if superControllerClassPackage??>
import ${superControllerClassPackage};
</#if>

/**
 * <p>
 * ${table.comment!} 前端控制器
 * </p>
 *
 * @author ${author}
 */
@Slf4j
@RequiredArgsConstructor
 <#if restControllerStyle>
@RestController
<#else>
@Controller
</#if>
@RequestMapping("/${table.tableName}")
<#if superControllerClass??>
public class ${className} extends ${superControllerClass} {
<#else>
public class ${className} {
</#if>

    private final ${table.tableNameUP}Service ${table.tableName}Service;

    @ApiOperation(value = "根据id查询信息")
    @GetMapping("/get/{id}")
    public CommonResult<${table.tableNameUP}> getById(@PathVariable("id") Long id) {
        return CommonResult.success(${table.tableName}Service.getById(id));
    }

    @ApiOperation(value = "新增数据")
    @PostMapping("/add")
    public CommonResult<Void> create(@RequestBody ${table.tableNameUP} ${table.tableName}) {
        ${table.tableName}Service.save(${table.tableName});
        return CommonResult.success();
    }

    @ApiOperation(value = "删除数据")
    @DeleteMapping("/delete/{id}")
    public CommonResult<Void> delete(@PathVariable("id") Long id) {
        ${table.tableName}Service.removeById(id);
        return CommonResult.success();
    }

    @ApiOperation(value = "更新数据")
    @PutMapping("/update")
    public CommonResult<Void> update(@RequestBody ${table.tableNameUP} ${table.tableName}) {
        ${table.tableName}Service.updateById(${table.tableName});
        return CommonResult.success();
    }

}
