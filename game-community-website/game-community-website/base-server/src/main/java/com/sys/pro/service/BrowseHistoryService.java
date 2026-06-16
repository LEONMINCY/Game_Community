package com.sys.pro.service;

import com.sys.pro.pojo.BrowseHistory;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * BrowseHistoryService 定义浏览历史业务能力，供控制器和其他服务组合调用。
 */
public interface BrowseHistoryService extends IService<BrowseHistory> {

    /**
     * 完成浏览历史中的 createBrowseHistory 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param browseHistory browseHistory 字段，来源于当前接口入参或内部调用上下文。
     */
    void createBrowseHistory(BrowseHistory browseHistory);

    /**
     * 完成浏览历史中的 deleteBrowseHistory 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param id 记录主键，用来定位本次要处理的数据。
     */
    void deleteBrowseHistory(Long id);

    /**
     * 完成浏览历史中的 updateBrowseHistory 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param browseHistory browseHistory 字段，来源于当前接口入参或内部调用上下文。
     */
    void updateBrowseHistory(BrowseHistory browseHistory);
}
