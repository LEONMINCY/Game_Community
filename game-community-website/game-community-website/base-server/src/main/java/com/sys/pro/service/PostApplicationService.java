package com.sys.pro.service;

import com.sys.pro.common.CommonResult;
import com.sys.pro.common.PageResult;
import com.sys.pro.dto.PostPageDTO;
import com.sys.pro.pojo.Post;
import com.sys.pro.vo.PostPageVo;
import com.sys.pro.vo.PostVo;

import java.util.List;
import java.util.Map;

/**
 * 帖子应用服务，负责社区帖子发布、审核、互动、搜索和展示聚合。
 */
public interface PostApplicationService {
    /**
     * 根据主键读取帖子详情，供详情页、弹窗或后台审核场景使用。
     * @param id 记录主键，用来定位本次要处理的数据。
     * @param currentUserId currentUser 主键，用来定位关联业务数据。
     * @param adminOrModerator adminOrModerator 字段，来源于当前接口入参或内部调用上下文。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */

    CommonResult<PostVo> getById(Integer id, Integer currentUserId, boolean adminOrModerator);
    /**
     * 接收新增帖子数据，完成入口校验后交由业务层保存。
     * @param post post 字段，来源于当前接口入参或内部调用上下文。
     * @param currentUserId currentUser 主键，用来定位关联业务数据。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */

    CommonResult<Void> create(Post post, Integer currentUserId);
    /**
     * 为后台管理端创建帖子数据，保留管理员发起操作的上下文。
     * @param post post 字段，来源于当前接口入参或内部调用上下文。
     * @param currentUserId currentUser 主键，用来定位关联业务数据。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */

    CommonResult<Void> adminCreate(Post post, Integer currentUserId);
    /**
     * 按主键删除帖子记录，并让业务层同步处理关联状态。
     * @param id 记录主键，用来定位本次要处理的数据。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */

    CommonResult<Void> delete(Long id);
    /**
     * 保存帖子编辑后的内容，让前台展示和后台管理保持一致。
     * @param post post 字段，来源于当前接口入参或内部调用上下文。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */

    CommonResult<Void> update(Post post);
    /**
     * 更新当前用户自己的帖子内容，并沿用个人帖子管理的权限边界。
     * @param post post 字段，来源于当前接口入参或内部调用上下文。
     * @param currentUserId currentUser 主键，用来定位关联业务数据。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */

    CommonResult<Void> updateMyPost(Post post, Integer currentUserId);
    /**
     * 删除当前用户自己的帖子，避免用户越权操作他人内容。
     * @param id 记录主键，用来定位本次要处理的数据。
     * @param currentUserId currentUser 主键，用来定位关联业务数据。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */

    CommonResult<Void> deleteMyPost(Integer id, Integer currentUserId);
    /**
     * 记录当前用户对帖子的点赞行为，并交由业务层维护计数。
     * @param id 记录主键，用来定位本次要处理的数据。
     * @param currentUserId currentUser 主键，用来定位关联业务数据。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */

    CommonResult<Void> like(Long id, Integer currentUserId);
    /**
     * 取消当前用户对帖子的点赞关系，并刷新对应计数。
     * @param id 记录主键，用来定位本次要处理的数据。
     * @param currentUserId currentUser 主键，用来定位关联业务数据。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */

    CommonResult<Void> unlike(Long id, Integer currentUserId);
    /**
     * 将帖子加入当前用户收藏列表，供个人中心后续查看。
     * @param id 记录主键，用来定位本次要处理的数据。
     * @param currentUserId currentUser 主键，用来定位关联业务数据。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */

    CommonResult<Void> favorite(Long id, Integer currentUserId);
    /**
     * 从当前用户收藏列表移除帖子，并同步收藏状态。
     * @param id 记录主键，用来定位本次要处理的数据。
     * @param currentUserId currentUser 主键，用来定位关联业务数据。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */

    CommonResult<Void> unFavorite(Long id, Integer currentUserId);
    /**
     * 记录帖子转发次数，便于前台展示转发热度。
     * @param id 记录主键，用来定位本次要处理的数据。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */

    CommonResult<Integer> sharePost(Integer id);
    /**
     * 根据用户行为和热度指标生成帖子推荐列表。
     * @param currentUserId currentUser 主键，用来定位关联业务数据。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */

    CommonResult<List<PostVo>> recommend(Integer currentUserId);
    /**
     * 分页查询帖子数据，返回列表内容和总数信息。
     * @param dto 请求 DTO，封装分页、筛选和排序条件。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */

    CommonResult<PageResult<PostPageVo>> page(PostPageDTO dto);
    /**
     * 按关键词检索帖子数据，给全局搜索或筛选框提供结果。
     * @param keyword 搜索关键词，支持按名称、内容或话题匹配。
     * @param gameId 游戏主键，用来定位被操作的游戏。
     * @param limit 返回数量上限，避免一次加载过多数据。
     * @param currentUserId currentUser 主键，用来定位关联业务数据。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */

    CommonResult<List<PostVo>> search(String keyword, Integer gameId, Integer limit, Integer currentUserId);
    /**
     * 统计近期高频话题，给搜索面板和社区热议区域展示。
     * @param limit 返回数量上限，避免一次加载过多数据。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */

    CommonResult<List<Map<String, Object>>> hotTopics(Integer limit);
    /**
     * 审核帖子发布状态，决定内容是否可以出现在社区列表中。
     * @param id 记录主键，用来定位本次要处理的数据。
     * @param body 请求体数据，承载本次操作需要的业务字段。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */

    CommonResult<Void> auditPost(Integer id, Map<String, String> body);
    /**
     * 提交帖子审核拒绝后的申诉内容。
     * @param id 记录主键，用来定位本次要处理的数据。
     * @param body 请求体数据，承载本次操作需要的业务字段。
     * @param currentUserId currentUser 主键，用来定位关联业务数据。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */

    CommonResult<Void> appealPost(Integer id, Map<String, String> body, Integer currentUserId);
    /**
     * 处理帖子申诉结果，并回写审核进度。
     * @param id 记录主键，用来定位本次要处理的数据。
     * @param body 请求体数据，承载本次操作需要的业务字段。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */

    CommonResult<Void> auditAppeal(Integer id, Map<String, String> body);
    /**
     * 按游戏标签查询社区帖子，展示对应游戏下的讨论内容。
     * @param gameId 游戏主键，用来定位被操作的游戏。
     * @param currentUserId currentUser 主键，用来定位关联业务数据。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */

    CommonResult<List<PostVo>> gamePost(Integer gameId, Integer currentUserId);
    /**
     * 查询指定用户公开的帖子动态。
     * @param userId 用户主键，用来限定当前操作的数据归属。
     * @param currentUserId currentUser 主键，用来定位关联业务数据。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */

    CommonResult<List<PostVo>> userPost(Integer userId, Integer currentUserId);
    /**
     * 查询当前用户发布的帖子，供我的帖子页面管理。
     * @param title title 字段，来源于当前接口入参或内部调用上下文。
     * @param auditStatus auditStatus 字段，来源于当前接口入参或内部调用上下文。
     * @param currentUserId currentUser 主键，用来定位关联业务数据。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */

    CommonResult<List<PostVo>> myPosts(String title, String auditStatus, Integer currentUserId);
    /**
     * 按游戏主键读取关联帖子，支撑游戏详情页社区讨论区。
     * @param gameId 游戏主键，用来定位被操作的游戏。
     * @param currentUserId currentUser 主键，用来定位关联业务数据。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */

    CommonResult<List<PostVo>> getPostsByGameId(Long gameId, Integer currentUserId);
    /**
     * 读取帖子的 PostsByGameName 数据，供页面展示或后续业务判断。
     * @param name name 字段，来源于当前接口入参或内部调用上下文。
     * @param currentUserId currentUser 主键，用来定位关联业务数据。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */

    CommonResult<List<PostVo>> getPostsByGameName(String name, Integer currentUserId);
    /**
     * 读取帖子的 Favorites 数据，供页面展示或后续业务判断。
     * @param userId 用户主键，用来限定当前操作的数据归属。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */

    CommonResult<List<PostVo>> getFavorites(Integer userId);
}
