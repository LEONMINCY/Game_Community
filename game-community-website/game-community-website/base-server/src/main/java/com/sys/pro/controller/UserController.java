package com.sys.pro.controller;

import com.sys.pro.common.CommonResult;
import com.sys.pro.common.PageResult;
import com.sys.pro.controller.common.BaseController;
import com.sys.pro.pojo.User;
import com.sys.pro.service.ReportService;
import com.sys.pro.service.UserService;
import com.sys.pro.vo.LoginParam;
import com.sys.pro.vo.LoginSuccessVo;
import com.sys.pro.vo.ReportVo;
import com.sys.pro.vo.UserParam;
import com.sys.pro.dto.UserPageDTO;
import com.sys.pro.vo.UserPageVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import io.swagger.annotations.ApiOperation;

/**
 * UserController 接收前端请求、校验入口参数并调用业务服务。
 */
@Slf4j
@RequiredArgsConstructor
@RestController
public class UserController extends BaseController {

    private final UserService userService;
    private final ReportService reportService;

    /**
     * 校验用户名或手机号登录信息，成功后签发访问令牌。
     * @param param 请求参数集合，承载前端提交的筛选或分页字段。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @PostMapping("/noLogin/login")
    public CommonResult<LoginSuccessVo> login(@RequestBody LoginParam param) {
        log.info("用户登录，参数:{}",param);
        final LoginSuccessVo loginResult = userService.login(param);
        return CommonResult.success(loginResult);
    }

    /**
     * 注册普通用户账号，并写入手机号、性别和年龄等基础资料。
     * @param user user 字段，来源于当前接口入参或内部调用上下文。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    /**
     * 登录被拦截时查询最近一次封禁或禁言处罚，用于登录页展示原因。
     * @param param 登录页提交的账号标识。
     * @return 最近一次处罚详情。
     */
    @PostMapping("/noLogin/ban-info")
    public CommonResult<ReportVo> banInfo(@RequestBody LoginParam param) {
        return CommonResult.success(reportService.getLatestPunishmentByIdentifier(param.getUsername()));
    }

    /**
     * 被封禁用户在登录页提交申诉，后台举报管理会同步显示申诉进度。
     * @param param 申诉内容和账号标识。
     * @return 通用成功响应。
     */
    @PostMapping("/noLogin/ban-appeal")
    public CommonResult<Void> banAppeal(@RequestBody ReportVo param) {
        reportService.appealLatestPunishmentByIdentifier(param.getReportedKeyword(), param.getAppealContent());
        return CommonResult.success();
    }

    @PostMapping("/noLogin/register")
    public CommonResult<Void> register(@RequestBody User user) {
        log.info("用户注册，参数:{}",user);
        userService.register(user);
        return CommonResult.success();
    }

    /**
     * 封禁/解封
     * /
    /**
     * 为后台管理端创建User数据，保留管理员发起操作的上下文。
     * @param userParam userParam 字段，来源于当前接口入参或内部调用上下文。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @PostMapping("/user/admin/add")
    public CommonResult<Void> adminCreate(@RequestBody UserParam userParam) {
        userService.createAccount(userParam);
        return CommonResult.success();
    }

    /**
     * 切换账号启用状态，用于后台冻结或恢复用户登录。
     * @param user user 字段，来源于当前接口入参或内部调用上下文。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @PutMapping("/user/enable")
    public CommonResult<Void> changeEnable(@RequestBody User user) {
        log.info("封禁/解封，参数：{}",user);
        userService.changeEnable(user);
        return CommonResult.success();
    }

    /**
     * 调整账号角色，并在对应角色账号表之间迁移数据。
     * @param user user 字段，来源于当前接口入参或内部调用上下文。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @PutMapping("/user/role")
    public CommonResult<Void> changeRole(@RequestBody User user) {
        userService.changeRole(user);
        return CommonResult.success();
    }

    /**
     * 按账号主键软删除用户，保留历史业务数据引用。
     * @param id 记录主键，用来定位本次要处理的数据。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @DeleteMapping("/user/{id}")
    public CommonResult<Void> deleteById(@PathVariable Long id) {
        log.info("删除用户，参数：{}", id);
        userService.deleteAccount(id);
        return CommonResult.success();
    }

    /**
     * 修改当前用户密码，校验旧密码后写入新哈希。
     * @param userParam userParam 字段，来源于当前接口入参或内部调用上下文。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @PostMapping("/update-password")
    public CommonResult<Void> updatePassword(@RequestBody UserParam userParam) {
        userParam.setUserId(this.getUserId());
        log.info("修改密码，参数：{}",userParam);
        userService.updatePassword(userParam);
        return CommonResult.success();
    }

    /**
     * 分页查询User数据，返回列表内容和总数信息。
     * @param dto 请求 DTO，封装分页、筛选和排序条件。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @ApiOperation(value = "分页查询用户列表")
    @PostMapping("/user/page")
    public CommonResult<PageResult<UserPageVo>> page(@RequestBody UserPageDTO dto) {
        return CommonResult.success(userService.pageUser(dto));
    }

    /**
     * 后台重置指定用户密码，用于用户无法自行找回时处理。
     * @param userId 用户主键，用来限定当前操作的数据归属。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @PostMapping("/user/reset-password/{id}")
    @ApiOperation(value = "重置用户密码")
    public CommonResult<Void> resetPassword(@PathVariable("id") Integer userId) {
        log.info("重置用户密码，用户ID：{}", userId);
        userService.resetPassword(userId);
        return CommonResult.success();
    }
}
