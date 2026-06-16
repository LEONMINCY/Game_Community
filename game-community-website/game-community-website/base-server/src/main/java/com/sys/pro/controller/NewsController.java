package com.sys.pro.controller;

import com.sys.pro.common.CommonResult;
import com.sys.pro.common.PageResult;
import com.sys.pro.controller.common.BaseController;
import com.sys.pro.dto.NewsPageDTO;
import com.sys.pro.pojo.News;
import com.sys.pro.pojo.NewsComment;
import com.sys.pro.pojo.UserInfo;
import com.sys.pro.service.NewsService;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * NewsController 接收前端请求、校验入口参数并调用业务服务。
 */
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/news")
public class NewsController extends BaseController {

    private final NewsService newsService;

    /**
     * 根据主键读取新闻详情，供详情页、弹窗或后台审核场景使用。
     * @param id 记录主键，用来定位本次要处理的数据。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @ApiOperation(value = "根据id查询新闻")
    @GetMapping("/get/{id}")
    public CommonResult<News> getById(@PathVariable("id") Integer id) {
        return CommonResult.success(newsService.getDetail(id, getUserId()));
    }

    /**
     * 接收新增新闻数据，完成入口校验后交由业务层保存。
     * @param news news 字段，来源于当前接口入参或内部调用上下文。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @ApiOperation(value = "新增新闻")
    @PostMapping("/add")
    public CommonResult<Void> create(@RequestBody News news) {
        newsService.createNews(news);
        return CommonResult.success();
    }

    /**
     * 按主键删除新闻记录，并让业务层同步处理关联状态。
     * @param id 记录主键，用来定位本次要处理的数据。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @ApiOperation(value = "删除新闻")
    @DeleteMapping("/delete/{id}")
    public CommonResult<Void> delete(@PathVariable("id") Long id) {
        newsService.deleteNews(id);
        return CommonResult.success();
    }

    /**
     * 保存新闻编辑后的内容，让前台展示和后台管理保持一致。
     * @param news news 字段，来源于当前接口入参或内部调用上下文。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @ApiOperation(value = "更新新闻")
    @PutMapping("/update")
    public CommonResult<Void> update(@RequestBody News news) {
        newsService.updateNews(news);
        return CommonResult.success();
    }

    /**
     * 分页查询新闻数据，返回列表内容和总数信息。
     * @param dto 请求 DTO，封装分页、筛选和排序条件。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @ApiOperation(value = "分页查询新闻列表")
    @PostMapping("/page")
    public CommonResult<PageResult<News>> page(@RequestBody NewsPageDTO dto) {
        return CommonResult.success(newsService.pageNews(dto, getUserId()));
    }

    /**
     * 记录当前用户对新闻的点赞行为，并交由业务层维护计数。
     * @param id 记录主键，用来定位本次要处理的数据。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @ApiOperation(value = "点赞新闻")
    @PostMapping("/like/{id}")
    public CommonResult<News> like(@PathVariable("id") Integer id) {
        return CommonResult.success(newsService.markLike(id, getUserId(), true));
    }

    /**
     * 取消当前用户对新闻的点赞关系，并刷新对应计数。
     * @param id 记录主键，用来定位本次要处理的数据。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @ApiOperation(value = "取消点赞新闻")
    @PostMapping("/unlike/{id}")
    public CommonResult<News> unlike(@PathVariable("id") Integer id) {
        return CommonResult.success(newsService.markLike(id, getUserId(), false));
    }

    /**
     * 将新闻加入当前用户收藏列表，供个人中心后续查看。
     * @param id 记录主键，用来定位本次要处理的数据。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @ApiOperation(value = "收藏新闻")
    @PostMapping("/favorite/{id}")
    public CommonResult<News> favorite(@PathVariable("id") Integer id) {
        return CommonResult.success(newsService.markFavorite(id, getUserId(), true));
    }

    /**
     * 从当前用户收藏列表移除新闻，并同步收藏状态。
     * @param id 记录主键，用来定位本次要处理的数据。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @ApiOperation(value = "取消收藏新闻")
    @PostMapping("/unFavorite/{id}")
    public CommonResult<News> unFavorite(@PathVariable("id") Integer id) {
        return CommonResult.success(newsService.markFavorite(id, getUserId(), false));
    }

    /**
     * 记录新闻转发行为，统计内容传播次数。
     * @param id 记录主键，用来定位本次要处理的数据。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @ApiOperation(value = "记录新闻转发")
    @PostMapping("/share/{id}")
    public CommonResult<Integer> share(@PathVariable("id") Integer id) {
        return CommonResult.success(newsService.shareNews(id));
    }

    /**
     * 读取新闻的 Favorites 数据，供页面展示或后续业务判断。
     * @param userId 用户主键，用来限定当前操作的数据归属。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @ApiOperation(value = "用户收藏的新闻")
    @GetMapping("/favorites/{userId}")
    public CommonResult<List<News>> getFavorites(@PathVariable("userId") Integer userId) {
        return CommonResult.success(newsService.listFavoriteNews(userId, getUserId()));
    }

    /**
     * 新增评论并处理艾特通知、图片和 AI 助手回复等后续动作。
     * @param comment comment 字段，来源于当前接口入参或内部调用上下文。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @ApiOperation(value = "新增新闻评论")
    @PostMapping("/comment/add")
    public CommonResult<Integer> addComment(@RequestBody NewsComment comment) {
        return CommonResult.success(newsService.addComment(comment, getUserId()));
    }

    /**
     * 汇总新闻列表数据，供前端列表、下拉框或统计模块使用。
     * @param param 请求参数集合，承载前端提交的筛选或分页字段。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @ApiOperation(value = "新闻评论列表")
    @PostMapping("/comment/list")
    public CommonResult<PageResult<NewsComment>> listComments(@RequestBody NewsComment param) {
        return CommonResult.success(newsService.listComments(param));
    }

    /**
     * 汇总新闻列表数据，供前端列表、下拉框或统计模块使用。
     * @param commentId 评论主键，用来定位被回复、举报或跳转的评论。
     * @param pageNo 当前页码，用于区分不同分页缓存。
     * @param pageSize 每页数量，用于控制分页列表长度。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @ApiOperation(value = "新闻评论回复列表")
    @GetMapping("/comment/reply/list/{commentId}")
    public CommonResult<PageResult<NewsComment>> listReplies(@PathVariable("commentId") Integer commentId,
                                                             @RequestParam(defaultValue = "1") Integer pageNo,
                                                             @RequestParam(defaultValue = "10") Integer pageSize) {
        return CommonResult.success(newsService.listReplies(commentId, pageNo, pageSize));
    }

    /**
     * 删除评论并同步刷新评论区展示状态。
     * @param id 记录主键，用来定位本次要处理的数据。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @ApiOperation(value = "删除新闻评论")
    @DeleteMapping("/comment/delete/{id}")
    public CommonResult<Void> deleteComment(@PathVariable("id") Integer id) {
        newsService.deleteComment(id, getUserId(), isAdminOrModerator());
        return CommonResult.success();
    }

    /**
     * 判断新闻当前状态是否满足业务条件。
     * @return true 表示新闻当前状态满足业务判断。
     */
    private boolean isAdminOrModerator() {
        UserInfo userInfo = getUserInfo();
        Integer roleId = userInfo.getRoleId();
        return Integer.valueOf(1).equals(roleId)
                || Integer.valueOf(3).equals(roleId)
                || (userInfo.getPermissions() != null && userInfo.getPermissions().contains("moderator"));
    }
}
