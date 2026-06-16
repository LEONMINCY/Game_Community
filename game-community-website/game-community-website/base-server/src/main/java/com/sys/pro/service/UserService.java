package com.sys.pro.service;

import com.sys.pro.common.PageResult;
import com.sys.pro.dto.UserPageDTO;
import com.sys.pro.pojo.User;
import com.sys.pro.vo.LoginParam;
import com.sys.pro.vo.LoginSuccessVo;
import com.sys.pro.vo.UserPageVo;
import com.sys.pro.vo.UserParam;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Unified account service across player, moderator and admin account tables.
 */
public interface UserService {
    /**
     * 校验用户名或手机号登录信息，成功后签发访问令牌。
     * @param param 请求参数集合，承载前端提交的筛选或分页字段。
     * @return User在该步骤产出的业务结果。
     */

    LoginSuccessVo login(LoginParam param);
    /**
     * 注册普通用户账号，并写入手机号、性别和年龄等基础资料。
     * @param user user 字段，来源于当前接口入参或内部调用上下文。
     */

    void register(User user);
    /**
     * 按用户名查询账号，支持登录和注册重复校验。
     * @param username username 字段，来源于当前接口入参或内部调用上下文。
     * @return User在该步骤产出的业务结果。
     */

    User findByUsername(String username);
    /**
     * 完成User中的 createAccount 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param userParam userParam 字段，来源于当前接口入参或内部调用上下文。
     */

    void createAccount(UserParam userParam);
    /**
     * 修改当前用户密码，校验旧密码后写入新哈希。
     * @param userParam userParam 字段，来源于当前接口入参或内部调用上下文。
     */

    void updatePassword(UserParam userParam);
    /**
     * 完成User中的 pageUser 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param dto 请求 DTO，封装分页、筛选和排序条件。
     * @return User分页结果，包含当前页数据和总数。
     */

    PageResult<UserPageVo> pageUser(UserPageDTO dto);
    /**
     * 后台重置指定用户密码，用于用户无法自行找回时处理。
     * @param userId 用户主键，用来限定当前操作的数据归属。
     */

    void resetPassword(Integer userId);

    /**
     * 切换账号启用状态，用于后台冻结或恢复用户登录。
     * @param user user 字段，来源于当前接口入参或内部调用上下文。
     */
    void changeEnable(User user);

    /**
     * 调整账号角色，并在对应角色账号表之间迁移数据。
     * @param user user 字段，来源于当前接口入参或内部调用上下文。
     */
    void changeRole(User user);

    /**
     * 完成User中的 deleteAccount 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param id 记录主键，用来定位本次要处理的数据。
     */
    void deleteAccount(Long id);
    /**
     * 根据主键读取User详情，供详情页、弹窗或后台审核场景使用。
     * @param id 记录主键，用来定位本次要处理的数据。
     * @return User在该步骤产出的业务结果。
     */

    User getById(Serializable id);
    /**
     * 保存User业务结果，并维护后续读取需要的一致状态。
     * @param user user 字段，来源于当前接口入参或内部调用上下文。
     * @return true 表示User当前状态满足业务判断。
     */

    boolean save(User user);
    /**
     * 按账号主键更新对应角色表中的账号信息。
     * @param user user 字段，来源于当前接口入参或内部调用上下文。
     * @return true 表示User当前状态满足业务判断。
     */

    boolean updateById(User user);
    /**
     * 移除User中的指定数据，并同步清理关联展示状态。
     * @param id 记录主键，用来定位本次要处理的数据。
     * @return true 表示User当前状态满足业务判断。
     */

    boolean removeById(Serializable id);
    /**
     * 完成User中的 count 步骤，保证该环节的数据和状态可以继续向下流转。
     * @return User统计值或主键结果。
     */

    int count();
    /**
     * 统计指定时间之后新增的账号数量。
     * @param start start 字段，来源于当前接口入参或内部调用上下文。
     * @return User统计值或主键结果。
     */

    long countCreatedAfter(LocalDateTime start);
    /**
     * 统计指定时间区间内新增的账号数量。
     * @param start start 字段，来源于当前接口入参或内部调用上下文。
     * @param end end 字段，来源于当前接口入参或内部调用上下文。
     * @return User统计值或主键结果。
     */

    long countCreatedBetween(LocalDateTime start, LocalDateTime end);
    /**
     * 统计最近登录过的账号数量，用于活跃度分析。
     * @param start start 字段，来源于当前接口入参或内部调用上下文。
     * @return User统计值或主键结果。
     */

    long countLastLoginAfter(LocalDateTime start);
}
