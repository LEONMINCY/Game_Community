package com.sys.pro.controller;

import com.sys.pro.common.CommonResult;
import com.sys.pro.controller.common.BaseController;
import com.sys.pro.pojo.UserRelation;
import com.sys.pro.service.UserRelationService;
import com.sys.pro.vo.UserInfoVo;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * UserRelationController 接收前端请求、校验入口参数并调用业务服务。
 */
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/userRelation")
public class UserRelationController extends BaseController {

    private final UserRelationService userRelationService;

    /**
     * 根据主键读取关注关系详情，供详情页、弹窗或后台审核场景使用。
     * @param id 记录主键，用来定位本次要处理的数据。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @ApiOperation(value = "根据id查询用户关系")
    @GetMapping("/get/{id}")
    public CommonResult<UserRelation> getById(@PathVariable("id") Long id) {
        return CommonResult.success(userRelationService.getById(id));
    }

    /**
     * 接收新增关注关系数据，完成入口校验后交由业务层保存。
     * @param userRelation userRelation 字段，来源于当前接口入参或内部调用上下文。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @ApiOperation(value = "新增用户关系")
    @PostMapping("/add")
    public CommonResult<Void> create(@RequestBody UserRelation userRelation) {
        userRelationService.createRelation(userRelation);
        return CommonResult.success();
    }

    /**
     * 按主键删除关注关系记录，并让业务层同步处理关联状态。
     * @param id 记录主键，用来定位本次要处理的数据。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @ApiOperation(value = "删除用户关系")
    @DeleteMapping("/delete/{id}")
    public CommonResult<Void> delete(@PathVariable("id") Long id) {
        userRelationService.deleteRelation(id);
        return CommonResult.success();
    }

    /**
     * 保存关注关系编辑后的内容，让前台展示和后台管理保持一致。
     * @param userRelation userRelation 字段，来源于当前接口入参或内部调用上下文。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @ApiOperation(value = "更新用户关系")
    @PutMapping("/update")
    public CommonResult<Void> update(@RequestBody UserRelation userRelation) {
        userRelationService.updateRelation(userRelation);
        return CommonResult.success();
    }

    /**
     * 关注目标用户，并更新双方关注和粉丝计数。
     * @param targetUserId targetUser 主键，用来定位关联业务数据。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @ApiOperation(value = "关注用户")
    @PostMapping("/follow/{targetUserId}")
    public CommonResult<Void> follow(@PathVariable("targetUserId") Integer targetUserId) {
        userRelationService.follow(getUserId(), targetUserId);
        return CommonResult.success();
    }

    /**
     * 取消关注目标用户，并同步刷新关系计数。
     * @param targetUserId targetUser 主键，用来定位关联业务数据。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @ApiOperation(value = "取消关注用户")
    @DeleteMapping("/unfollow/{targetUserId}")
    public CommonResult<Void> unfollow(@PathVariable("targetUserId") Integer targetUserId) {
        userRelationService.unfollow(getUserId(), targetUserId);
        return CommonResult.success();
    }

    /**
     * 读取关注关系的 FollowingList 数据，供页面展示或后续业务判断。
     * @param userId 用户主键，用来限定当前操作的数据归属。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @ApiOperation(value = "获取用户关注列表")
    @GetMapping("/following/{userId}")
    public CommonResult<List<UserInfoVo>> getFollowingList(@PathVariable("userId") Integer userId) {
        if (!userRelationService.canViewRelationList(getUserId(), userId, true)) {
            return CommonResult.error(403, "该用户已隐藏关注列表");
        }
        return CommonResult.success(userRelationService.getFollowingList(userId));
    }

    /**
     * 读取关注关系的 FollowersList 数据，供页面展示或后续业务判断。
     * @param userId 用户主键，用来限定当前操作的数据归属。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @ApiOperation(value = "获取用户粉丝列表")
    @GetMapping("/followers/{userId}")
    public CommonResult<List<UserInfoVo>> getFollowersList(@PathVariable("userId") Integer userId) {
        if (!userRelationService.canViewRelationList(getUserId(), userId, false)) {
            return CommonResult.error(403, "该用户已隐藏粉丝列表");
        }
        return CommonResult.success(userRelationService.getFollowersList(userId));
    }
}
