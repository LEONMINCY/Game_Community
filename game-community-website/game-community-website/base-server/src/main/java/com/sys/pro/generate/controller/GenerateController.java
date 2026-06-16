package com.sys.pro.generate.controller;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.annotation.TableName;
import com.sys.pro.common.CommonResult;
import com.sys.pro.generate.ComponentGenerator;
import com.sys.pro.generate.pojo.DialogFieldInfo;
import com.sys.pro.generate.pojo.DialogInfo;
import com.sys.pro.generate.strategy.ComponentType;
import io.swagger.annotations.ApiModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.reflections.Reflections;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static com.sys.pro.common.CommonResult.success;
/**
 * 代码生成入口，读取实体元数据并生成前端表单配置。
 */



@Slf4j

@RestController

@RequiredArgsConstructor

@RequestMapping("/generate")
public class GenerateController {



    private final ComponentGenerator componentGenerator;



    /**
     * 读取Generate列表数据，按页面传入条件完成筛选和排序。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @GetMapping("/list")
    public CommonResult<List<JSONObject>> list() {
        log.info("开始查询可生成的类列表");
        // 获取pojo包下的类
        String packageName = "com.sys.pro.pojo";

        // 使用Reflections库获取包下所有类
        Reflections reflections = new Reflections(packageName);
        Set<Class<?>> classes = reflections.getTypesAnnotatedWith(TableName.class);

        List<JSONObject> list = new ArrayList<>();

        for (Class<?> clazz : classes) {
            JSONObject classInfo = new JSONObject();

            // 获取类说明
            ApiModel apiModel = clazz.getAnnotation(ApiModel.class);
            String description = apiModel.description();

            classInfo.put("className", clazz.getSimpleName().toLowerCase(Locale.ROOT));
            classInfo.put("description", description);
            classInfo.put("classPath", clazz.getCanonicalName());
            list.add(classInfo);
        }
        return success(list);
    }

    /**
     * 读取Generate的 Code 数据，供页面展示或后续业务判断。
     * @param dialogInfo dialogInfo 字段，来源于当前接口入参或内部调用上下文。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @PostMapping("/dialog")
    public CommonResult<String> getCode(@RequestBody DialogInfo dialogInfo) {
        log.info("开始生成dialog代码，参数:{}", dialogInfo);
        String result = componentGenerator.generate(ComponentType.DIALOG, dialogInfo);
        return success(result);
    }

    /**
     * 读取Generate的 Fields 数据，供页面展示或后续业务判断。
     * @param param 请求参数集合，承载前端提交的筛选或分页字段。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @PostMapping("/fields")
    public CommonResult<List<DialogFieldInfo>> getFields(@RequestBody JSONObject param) {
        log.info("获取字段列表，参数:{}", param);
        String className = param.getString("className");
        List<DialogFieldInfo> dialogFieldInfos = componentGenerator.extractDialogFields(className);
        return success(dialogFieldInfos);
    }

}
