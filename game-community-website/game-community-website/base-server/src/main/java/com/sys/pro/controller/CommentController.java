package com.sys.pro.controller;

import com.sys.pro.common.PageResult;
import com.sys.pro.service.CommentService;
import com.sys.pro.pojo.Comment;
import com.sys.pro.vo.CommentVo;
import io.swagger.annotations.ApiOperation;
import com.sys.pro.common.CommonResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.*;
import com.sys.pro.controller.common.BaseController;

/**
 * CommentController 接收前端请求、校验入口参数并调用业务服务。
 */
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/comment")
public class CommentController extends BaseController {



    private final CommentService commentService;

    /**
     * 根据主键读取评论详情，供详情页、弹窗或后台审核场景使用。
     * @param id 记录主键，用来定位本次要处理的数据。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @ApiOperation(value = "根据id查询信息")
    @GetMapping("/get/{id}")
    public CommonResult<Comment> getById(@PathVariable("id") Long id) {
        return CommonResult.success(commentService.getById(id));
    }

    /**
     * 接收新增评论数据，完成入口校验后交由业务层保存。
     * @param comment comment 字段，来源于当前接口入参或内部调用上下文。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @ApiOperation(value = "新增评论")
    @PostMapping("/add")
    public CommonResult<Integer> create(@RequestBody Comment comment) {
        return CommonResult.success(commentService.createComment(comment, getUserId()));
    }

    /**
     * 按主键删除评论记录，并让业务层同步处理关联状态。
     * @param id 记录主键，用来定位本次要处理的数据。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @ApiOperation(value = "删除评论")
    @DeleteMapping("/delete/{id}")
    public CommonResult<Void> delete(@PathVariable("id") Long id) {
        commentService.deleteComment(id);
        return CommonResult.success();
    }

    /**
     * 保存评论编辑后的内容，让前台展示和后台管理保持一致。
     * @param comment comment 字段，来源于当前接口入参或内部调用上下文。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @ApiOperation(value = "更新评论")
    @PutMapping("/update")
    public CommonResult<Void> update(@RequestBody Comment comment) {
        commentService.updateComment(comment);
        return CommonResult.success();
    }

    /**
     * 读取评论列表数据，按页面传入条件完成筛选和排序。
     * @param param 请求参数集合，承载前端提交的筛选或分页字段。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @ApiOperation(value = "分页查询评论列表")
    @PostMapping("/list")
    public CommonResult<PageResult<CommentVo>> list(@RequestBody CommentVo param) {
        param.setCurrentUserId(getUserId());
        PageResult<CommentVo> pageList = commentService.getPageListByPostId(param);
        return CommonResult.success(pageList);
    }

    /**
     * 点赞指定评论，成功后返回评论最新点赞数，供前端即时刷新按钮状态。
     * @param id 评论ID。
     * @return 最新点赞数。
     */
    @ApiOperation(value = "点赞评论")
    @PostMapping("/like/{id}")
    public CommonResult<Integer> like(@PathVariable("id") Integer id) {
        return CommonResult.success(commentService.likeComment(id, getUserId()));
    }

    /**
     * 取消指定评论点赞，成功后返回评论最新点赞数，供前端即时刷新按钮状态。
     * @param id 评论ID。
     * @return 最新点赞数。
     */
    @ApiOperation(value = "取消评论点赞")
    @PostMapping("/unlike/{id}")
    public CommonResult<Integer> unlike(@PathVariable("id") Integer id) {
        return CommonResult.success(commentService.unlikeComment(id, getUserId()));
    }

    /**
     * 读取评论的 ReplyList 数据，供页面展示或后续业务判断。
     * @param commentId 评论主键，用来定位被回复、举报或跳转的评论。
     * @param pageNo 当前页码，用于区分不同分页缓存。
     * @param pageSize 每页数量，用于控制分页列表长度。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @ApiOperation(value = "获取评论的回复列表")
    @GetMapping("/reply/list/{commentId}")
    public CommonResult<PageResult<CommentVo>> getReplyList(
            @PathVariable("commentId") Integer commentId,
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        PageResult<CommentVo> replyList = commentService.getReplyList(commentId, pageNo, pageSize, getUserId());
        return CommonResult.success(replyList);
    }

    /**
     * 查询当前用户发表过的评论，供通知中心“我的评论”页签展示。
     * @param pageNo 当前页码，用于分页加载。
     * @param pageSize 每页数量，用于控制单次返回规模。
     * @return 包装后的接口响应，包含当前用户评论列表。
     */
    @ApiOperation(value = "获取我的评论列表")
    @GetMapping("/my/list")
    public CommonResult<PageResult<CommentVo>> getMyCommentList(
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return CommonResult.success(commentService.getMyCommentList(getUserId(), pageNo, pageSize));
    }
}
