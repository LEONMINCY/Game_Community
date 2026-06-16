package com.sys.pro.mapper;

import com.sys.pro.pojo.UserInfo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * UserInfoMapper 是用户资料的数据访问入口，负责MyBatis-Plus基础读写和扩展查询。
 */
public interface UserInfoMapper extends BaseMapper<UserInfo> {
    /**
     * 读取用户资料的 UserInfo 数据，供页面展示或后续业务判断。
     * @param userId 用户主键，用来限定当前操作的数据归属。
     * @return 用户资料在该步骤产出的业务结果。
     */

    UserInfo getUserInfo(Integer userId);
    /**
     * 按用户名、昵称或手机号检索用户，供全局搜索和关注选择使用。
     * @param keyword 搜索关键词，支持按名称、内容或话题匹配。
     * @param limit 返回数量上限，避免一次加载过多数据。
     * @return 用户资料列表数据。
     */

    List<UserInfo> searchUsers(@Param("keyword") String keyword, @Param("limit") Integer limit);
}
