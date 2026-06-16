package com.sys.pro.service;

import com.sys.pro.common.PageResult;
import com.sys.pro.pojo.Comment;
import com.baomidou.mybatisplus.extension.service.IService;
import com.sys.pro.vo.CommentVo;

/**
 * CommentService 定义帖子评论业务能力，供控制器和其他服务组合调用。
 */
public interface CommentService extends IService<Comment> {



    /**
     * 读取评论的 PageListByPostId 数据，供页面展示或后续业务判断。
     * @param param 请求参数集合，承载前端提交的筛选或分页字段。
     * @return 评论分页结果，包含当前页数据和总数。
     */

    PageResult<CommentVo> getPageListByPostId(CommentVo param);



    /**
     * 新增评论并处理艾特通知、图片和 AI 助手回复等后续动作。
     * @param comment comment 字段，来源于当前接口入参或内部调用上下文。
     * @return 评论统计值或主键结果。
     */

    Integer addComment(Comment comment);

    /**
     * 完成评论中的 createComment 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param comment comment 字段，来源于当前接口入参或内部调用上下文。
     * @param currentUserId currentUser 主键，用来定位关联业务数据。
     * @return 评论统计值或主键结果。
     */
    Integer createComment(Comment comment, Integer currentUserId);

    /**
     * 删除评论并同步刷新评论区展示状态。
     * @param id 记录主键，用来定位本次要处理的数据。
     */
    void deleteComment(Long id);

    /**
     * 完成评论中的 updateComment 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param comment comment 字段，来源于当前接口入参或内部调用上下文。
     */
    void updateComment(Comment comment);


    /**
     * 读取评论的 ReplyList 数据，供页面展示或后续业务判断。
     * @param commentId 评论主键，用来定位被回复、举报或跳转的评论。
     * @param pageNo 当前页码，用于区分不同分页缓存。
     * @param pageSize 每页数量，用于控制分页列表长度。
     * @return 评论分页结果，包含当前页数据和总数。
     */

    PageResult<CommentVo> getReplyList(Integer commentId, Integer pageNo, Integer pageSize);

    /**
     * 查询指定评论下的回复，并带上当前用户的点赞状态。
     * @param commentId 父评论ID。
     * @param pageNo 当前页码。
     * @param pageSize 每页数量。
     * @param currentUserId 当前登录用户ID，游客访问时可以为空。
     * @return 回复评论分页结果。
     */
    PageResult<CommentVo> getReplyList(Integer commentId, Integer pageNo, Integer pageSize, Integer currentUserId);

    /**
     * 给评论点赞，重复点赞时只返回当前点赞数，不重复写入用户ID。
     * @param commentId 被点赞评论ID。
     * @param currentUserId 当前登录用户ID。
     * @return 点赞后的点赞数。
     */
    Integer likeComment(Integer commentId, Integer currentUserId);

    /**
     * 取消评论点赞，未点赞时保持幂等，不抛出业务异常。
     * @param commentId 被取消点赞评论ID。
     * @param currentUserId 当前登录用户ID。
     * @return 取消后的点赞数。
     */
    Integer unlikeComment(Integer commentId, Integer currentUserId);

    /**
     * 查询当前用户发表过的评论，给通知中心“我的评论”页签展示。
     * @param userId 当前登录用户主键。
     * @param pageNo 当前页码。
     * @param pageSize 每页数量。
     * @return 评论分页结果，包含评论内容和所属帖子摘要。
     */
    PageResult<CommentVo> getMyCommentList(Integer userId, Integer pageNo, Integer pageSize);

}

