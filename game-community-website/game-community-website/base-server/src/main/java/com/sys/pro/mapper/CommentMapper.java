package com.sys.pro.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sys.pro.pojo.Comment;
import com.sys.pro.vo.CommentVo;
import org.apache.ibatis.annotations.Param;

/**
 * CommentMapper 是帖子评论的数据访问入口，负责MyBatis-Plus基础读写和扩展查询。
 */
public interface CommentMapper extends BaseMapper<Comment> {



    /**
     * 读取评论的 PageListByPostId 数据，供页面展示或后续业务判断。
     * @param page page 字段，来源于当前接口入参或内部调用上下文。
     * @param param 请求参数集合，承载前端提交的筛选或分页字段。
     * @return 评论分页结果，包含当前页数据和总数。
     */

    Page<CommentVo> getPageListByPostId(Page<CommentVo> page, @Param("param") CommentVo param);



    /**
     * 读取评论的 PageListByParentId 数据，供页面展示或后续业务判断。
     * @param page page 字段，来源于当前接口入参或内部调用上下文。
     * @param param 请求参数集合，承载前端提交的筛选或分页字段。
     * @return 评论分页结果，包含当前页数据和总数。
     */

    Page<CommentVo> getPageListByParentId(Page<CommentVo> page, @Param("param") CommentVo param);

    /**
     * 查询指定用户发表过的评论，并携带所属帖子摘要。
     * @param page 分页对象。
     * @param userId 当前用户主键。
     * @return 用户评论分页结果。
     */
    Page<CommentVo> getMyCommentList(Page<CommentVo> page, @Param("userId") Integer userId);

}

