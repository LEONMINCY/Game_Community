package com.sys.pro.controller;

import com.sys.pro.common.CommonResult;
import com.sys.pro.common.PageResult;
import com.sys.pro.controller.common.BaseController;
import com.sys.pro.pojo.Game;
import com.sys.pro.service.GameApplicationService;
import com.sys.pro.vo.GameVo;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 游戏中心接口层。
 * 只负责请求参数、当前用户上下文和响应包装；游戏业务统一委托给 GameApplicationService。
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/game")
public class GameController extends BaseController {

    private final GameApplicationService gameApplicationService;

    /**
     * 根据主键读取游戏详情，供详情页、弹窗或后台审核场景使用。
     * @param id 记录主键，用来定位本次要处理的数据。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @ApiOperation(value = "根据id查询信息")
    @GetMapping("/get/{id}")
    public CommonResult<GameVo> getById(@PathVariable("id") Integer id) {
        return CommonResult.success(gameApplicationService.getById(id, getUserId()));
    }

    /**
     * 接收新增游戏数据，完成入口校验后交由业务层保存。
     * @param game game 字段，来源于当前接口入参或内部调用上下文。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @ApiOperation(value = "新增数据")
    @PostMapping("/add")
    public CommonResult<Void> create(@RequestBody Game game) {
        gameApplicationService.create(game);
        return CommonResult.success();
    }

    /**
     * 按主键删除游戏记录，并让业务层同步处理关联状态。
     * @param id 记录主键，用来定位本次要处理的数据。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @ApiOperation(value = "删除数据")
    @DeleteMapping("/delete/{id}")
    public CommonResult<Void> delete(@PathVariable("id") Long id) {
        gameApplicationService.delete(id);
        return CommonResult.success();
    }

    /**
     * 保存游戏编辑后的内容，让前台展示和后台管理保持一致。
     * @param game game 字段，来源于当前接口入参或内部调用上下文。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @ApiOperation(value = "更新数据")
    @PostMapping("/update")
    public CommonResult<Void> update(@RequestBody Game game) {
        gameApplicationService.update(game);
        return CommonResult.success();
    }

    /**
     * 读取全部可用游戏数据，供下拉框或初始化页面使用。
     * @param keyword 搜索关键词，支持按名称、内容或话题匹配。
     * @param limit 返回数量上限，避免一次加载过多数据。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @ApiOperation(value = "查询全部游戏id和名字")
    @GetMapping("/listAll")
    public CommonResult<List<Game>> listAll(String keyword, Integer limit) {
        return CommonResult.success(gameApplicationService.listAll(keyword, limit));
    }

    /**
     * 读取游戏列表数据，按页面传入条件完成筛选和排序。
     * @param game game 字段，来源于当前接口入参或内部调用上下文。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @ApiOperation(value = "分页查询游戏列表")
    @PostMapping("/list")
    public CommonResult<PageResult<Game>> list(@RequestBody GameVo game) {
        return CommonResult.success(gameApplicationService.list(game));
    }

    /**
     * 根据用户行为和热度指标生成游戏推荐列表。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @ApiOperation(value = "个性化游戏推荐列表")
    @GetMapping("/recommend")
    public CommonResult<List<Game>> recommend() {
        return CommonResult.success(gameApplicationService.recommend(getUserId()));
    }

    /**
     * 按销量、好评率或业务热度读取热门游戏列表。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @ApiOperation(value = "获取热门游戏列表")
    @GetMapping("/listHot")
    public CommonResult<List<Game>> listHot() {
        return CommonResult.success(gameApplicationService.listHot());
    }

    /**
     * 按发帖量统计热门社区，供首页侧栏跳转到对应帖子列表。
     * @param limit 返回数量上限，避免一次加载过多数据。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @ApiOperation(value = "获取热门社区列表")
    @GetMapping("/hotCommunities")
    public CommonResult<List<Game>> hotCommunities(@RequestParam(defaultValue = "5") Integer limit) {
        return CommonResult.success(gameApplicationService.hotCommunities(limit));
    }

    /**
     * 读取游戏类型标签，供前台筛选和后台表单复用。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @ApiOperation(value = "获取游戏类型")
    @GetMapping("/types")
    public CommonResult<List<String>> types() {
        return CommonResult.success(gameApplicationService.types());
    }

    /**
     * 读取正在打折的游戏列表，展示限时优惠入口。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @ApiOperation(value = "获取折扣游戏列表")
    @GetMapping("/discounts")
    public CommonResult<List<Game>> discounts() {
        return CommonResult.success(gameApplicationService.discounts());
    }

    /**
     * 汇总游戏详情页需要的评分、销量和趋势统计。
     * @param id 记录主键，用来定位本次要处理的数据。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @ApiOperation(value = "获取游戏数据统计")
    @GetMapping("/stats/{id}")
    public CommonResult<Map<String, Object>> stats(@PathVariable("id") Integer id) {
        return CommonResult.success(gameApplicationService.stats(id));
    }
}
