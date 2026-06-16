package com.sys.pro.controller;

import com.sys.pro.service.BrowseHistoryService;
import com.sys.pro.pojo.BrowseHistory;
import io.swagger.annotations.ApiOperation;
import com.sys.pro.common.CommonResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.*;
import com.sys.pro.controller.common.BaseController;

/**
 * BrowseHistoryController 接收前端请求、校验入口参数并调用业务服务。
 */
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/browseHistory")
public class BrowseHistoryController extends BaseController {



    private final BrowseHistoryService browseHistoryService;



    /**
     * 根据主键读取浏览历史详情，供详情页、弹窗或后台审核场景使用。
     * @param id 记录主键，用来定位本次要处理的数据。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @ApiOperation(value = "根据id查询信息")
    @GetMapping("/get/{id}")
    public CommonResult<BrowseHistory> getById(@PathVariable("id") Long id) {
        return CommonResult.success(browseHistoryService.getById(id));
    }

    /**
     * 接收新增浏览历史数据，完成入口校验后交由业务层保存。
     * @param browseHistory browseHistory 字段，来源于当前接口入参或内部调用上下文。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @ApiOperation(value = "新增数据")
    @PostMapping("/add")
    public CommonResult<Void> create(@RequestBody BrowseHistory browseHistory) {
        browseHistoryService.createBrowseHistory(browseHistory);
        return CommonResult.success();
    }

    /**
     * 按主键删除浏览历史记录，并让业务层同步处理关联状态。
     * @param id 记录主键，用来定位本次要处理的数据。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @ApiOperation(value = "删除数据")
    @DeleteMapping("/delete/{id}")
    public CommonResult<Void> delete(@PathVariable("id") Long id) {
        browseHistoryService.deleteBrowseHistory(id);
        return CommonResult.success();
    }

    /**
     * 保存浏览历史编辑后的内容，让前台展示和后台管理保持一致。
     * @param browseHistory browseHistory 字段，来源于当前接口入参或内部调用上下文。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @ApiOperation(value = "更新数据")
    @PutMapping("/update")
    public CommonResult<Void> update(@RequestBody BrowseHistory browseHistory) {
        browseHistoryService.updateBrowseHistory(browseHistory);
        return CommonResult.success();
    }

}
