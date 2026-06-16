package com.sys.pro.service.impl;

import com.sys.pro.pojo.BrowseHistory;
import com.sys.pro.service.BrowseHistoryService;
import com.sys.pro.mapper.BrowseHistoryMapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 浏览历史服务实现，记录并查询用户访问帖子、新闻和游戏的历史。
 */
@Slf4j

@Service

@RequiredArgsConstructor
public class BrowseHistoryServiceImpl extends ServiceImpl<BrowseHistoryMapper, BrowseHistory> implements BrowseHistoryService {

    /**
     * 完成浏览历史中的 createBrowseHistory 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param browseHistory browseHistory 字段，来源于当前接口入参或内部调用上下文。
     */
    @Override
    public void createBrowseHistory(BrowseHistory browseHistory) {
        save(browseHistory);
    }

    /**
     * 完成浏览历史中的 deleteBrowseHistory 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param id 记录主键，用来定位本次要处理的数据。
     */
    @Override
    public void deleteBrowseHistory(Long id) {
        removeById(id);
    }

    /**
     * 完成浏览历史中的 updateBrowseHistory 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param browseHistory browseHistory 字段，来源于当前接口入参或内部调用上下文。
     */
    @Override
    public void updateBrowseHistory(BrowseHistory browseHistory) {
        updateById(browseHistory);
    }
}
