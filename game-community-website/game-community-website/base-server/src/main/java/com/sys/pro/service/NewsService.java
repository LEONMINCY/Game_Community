package com.sys.pro.service;

import com.sys.pro.pojo.News;
import com.baomidou.mybatisplus.extension.service.IService;
import com.sys.pro.common.PageResult;
import com.sys.pro.dto.NewsPageDTO;
import com.sys.pro.pojo.NewsComment;

import java.util.List;

/**
 * NewsService 定义新闻业务能力，供控制器和其他服务组合调用。
 */
public interface NewsService extends IService<News> {

    /**
     * 完成新闻中的 createNews 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param news news 字段，来源于当前接口入参或内部调用上下文。
     */
    void createNews(News news);

    /**
     * 完成新闻中的 updateNews 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param news news 字段，来源于当前接口入参或内部调用上下文。
     */
    void updateNews(News news);

    /**
     * 读取新闻的 Detail 数据，供页面展示或后续业务判断。
     * @param id 记录主键，用来定位本次要处理的数据。
     * @param currentUserId currentUser 主键，用来定位关联业务数据。
     * @return 新闻在该步骤产出的业务结果。
     */
    News getDetail(Integer id, Integer currentUserId);

    /**
     * 完成新闻中的 deleteNews 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param id 记录主键，用来定位本次要处理的数据。
     */
    void deleteNews(Long id);

    /**
     * 完成新闻中的 pageNews 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param dto 请求 DTO，封装分页、筛选和排序条件。
     * @param currentUserId currentUser 主键，用来定位关联业务数据。
     * @return 新闻分页结果，包含当前页数据和总数。
     */
    PageResult<News> pageNews(NewsPageDTO dto, Integer currentUserId);

    /**
     * 完成新闻中的 markLike 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param id 记录主键，用来定位本次要处理的数据。
     * @param currentUserId currentUser 主键，用来定位关联业务数据。
     * @param add add 字段，来源于当前接口入参或内部调用上下文。
     * @return 新闻在该步骤产出的业务结果。
     */
    News markLike(Integer id, Integer currentUserId, boolean add);

    /**
     * 完成新闻中的 markFavorite 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param id 记录主键，用来定位本次要处理的数据。
     * @param currentUserId currentUser 主键，用来定位关联业务数据。
     * @param add add 字段，来源于当前接口入参或内部调用上下文。
     * @return 新闻在该步骤产出的业务结果。
     */
    News markFavorite(Integer id, Integer currentUserId, boolean add);

    /**
     * 完成新闻中的 shareNews 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param id 记录主键，用来定位本次要处理的数据。
     * @return 新闻统计值或主键结果。
     */
    Integer shareNews(Integer id);

    /**
     * 汇总新闻列表数据，供前端列表、下拉框或统计模块使用。
     * @param userId 用户主键，用来限定当前操作的数据归属。
     * @param currentUserId currentUser 主键，用来定位关联业务数据。
     * @return 新闻列表数据。
     */
    List<News> listFavoriteNews(Integer userId, Integer currentUserId);

    /**
     * 新增评论并处理艾特通知、图片和 AI 助手回复等后续动作。
     * @param comment comment 字段，来源于当前接口入参或内部调用上下文。
     * @param currentUserId currentUser 主键，用来定位关联业务数据。
     * @return 新闻统计值或主键结果。
     */
    Integer addComment(NewsComment comment, Integer currentUserId);

    /**
     * 汇总新闻列表数据，供前端列表、下拉框或统计模块使用。
     * @param param 请求参数集合，承载前端提交的筛选或分页字段。
     * @return 新闻分页结果，包含当前页数据和总数。
     */
    PageResult<NewsComment> listComments(NewsComment param);

    /**
     * 汇总新闻列表数据，供前端列表、下拉框或统计模块使用。
     * @param commentId 评论主键，用来定位被回复、举报或跳转的评论。
     * @param pageNo 当前页码，用于区分不同分页缓存。
     * @param pageSize 每页数量，用于控制分页列表长度。
     * @return 新闻分页结果，包含当前页数据和总数。
     */
    PageResult<NewsComment> listReplies(Integer commentId, Integer pageNo, Integer pageSize);

    /**
     * 删除评论并同步刷新评论区展示状态。
     * @param id 记录主键，用来定位本次要处理的数据。
     * @param currentUserId currentUser 主键，用来定位关联业务数据。
     * @param adminOrModerator adminOrModerator 字段，来源于当前接口入参或内部调用上下文。
     */
    void deleteComment(Integer id, Integer currentUserId, boolean adminOrModerator);
}
