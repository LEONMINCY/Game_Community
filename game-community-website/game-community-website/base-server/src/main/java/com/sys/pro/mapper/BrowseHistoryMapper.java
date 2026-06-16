package com.sys.pro.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sys.pro.pojo.BrowseHistory;
import io.lettuce.core.dynamic.annotation.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * BrowseHistoryMapper 是浏览历史的数据访问入口，负责MyBatis-Plus基础读写和扩展查询。
 */
public interface BrowseHistoryMapper extends BaseMapper<BrowseHistory> {
    /**
     * 读取浏览历史的 UserScores 数据，供页面展示或后续业务判断。
     * @param targetType targetType 字段，来源于当前接口入参或内部调用上下文。
     * @return 浏览历史列表数据。
     */



    List<Map<String, Object>> getUserScores(@Param("targetType") String targetType);

}

