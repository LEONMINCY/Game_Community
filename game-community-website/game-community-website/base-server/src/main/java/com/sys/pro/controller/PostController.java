package com.sys.pro.controller;

import com.sys.pro.common.CommonResult;
import com.sys.pro.common.PageResult;
import com.sys.pro.controller.common.BaseController;
import com.sys.pro.dto.PostPageDTO;
import com.sys.pro.pojo.Post;
import com.sys.pro.pojo.UserInfo;
import com.sys.pro.service.PostApplicationService;
import com.sys.pro.vo.PostPageVo;
import com.sys.pro.vo.PostVo;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * PostController 接收前端请求、校验入口参数并调用业务服务。
 */
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/post")
public class PostController extends BaseController {

    private final PostApplicationService postApplicationService;

    /**
     * 根据主键读取帖子详情，供详情页、弹窗或后台审核场景使用。
     * @param id 记录主键，用来定位本次要处理的数据。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @ApiOperation(value = "根据id查询帖子")
    @GetMapping("/get/{id}")
    public CommonResult<PostVo> getById(@PathVariable("id") Integer id) {
        return postApplicationService.getById(id, getUserId(), isAdminOrModerator());
    }

    /**
     * 接收新增帖子数据，完成入口校验后交由业务层保存。
     * @param post post 字段，来源于当前接口入参或内部调用上下文。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @ApiOperation(value = "新增帖子")
    @PostMapping("/add")
    public CommonResult<Void> create(@RequestBody Post post) {
        return postApplicationService.create(post, getUserId());
    }

    /**
     * 为后台管理端创建帖子数据，保留管理员发起操作的上下文。
     * @param post post 字段，来源于当前接口入参或内部调用上下文。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @ApiOperation(value = "后台新增帖子")
    @PostMapping("/admin/add")
    public CommonResult<Void> adminCreate(@RequestBody Post post) {
        return postApplicationService.adminCreate(post, getUserId());
    }

    /**
     * 按主键删除帖子记录，并让业务层同步处理关联状态。
     * @param id 记录主键，用来定位本次要处理的数据。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @ApiOperation(value = "删除帖子")
    @DeleteMapping("/delete/{id}")
    public CommonResult<Void> delete(@PathVariable("id") Long id) {
        return postApplicationService.delete(id);
    }

    /**
     * 保存帖子编辑后的内容，让前台展示和后台管理保持一致。
     * @param post post 字段，来源于当前接口入参或内部调用上下文。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @ApiOperation(value = "更新帖子")
    @PutMapping("/update")
    public CommonResult<Void> update(@RequestBody Post post) {
        return postApplicationService.update(post);
    }

    /**
     * 更新当前用户自己的帖子内容，并沿用个人帖子管理的权限边界。
     * @param post post 字段，来源于当前接口入参或内部调用上下文。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @ApiOperation(value = "用户更新自己的帖子")
    @PutMapping("/my/update")
    public CommonResult<Void> updateMyPost(@RequestBody Post post) {
        return postApplicationService.updateMyPost(post, getUserId());
    }

    /**
     * 删除当前用户自己的帖子，避免用户越权操作他人内容。
     * @param id 记录主键，用来定位本次要处理的数据。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @ApiOperation(value = "用户删除自己的帖子")
    @DeleteMapping("/my/delete/{id}")
    public CommonResult<Void> deleteMyPost(@PathVariable("id") Integer id) {
        return postApplicationService.deleteMyPost(id, getUserId());
    }

    /**
     * 记录当前用户对帖子的点赞行为，并交由业务层维护计数。
     * @param id 记录主键，用来定位本次要处理的数据。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @ApiOperation(value = "点赞帖子")
    @PostMapping("/like/{id}")
    public CommonResult<Void> like(@PathVariable("id") Long id) {
        return postApplicationService.like(id, getUserId());
    }

    /**
     * 取消当前用户对帖子的点赞关系，并刷新对应计数。
     * @param id 记录主键，用来定位本次要处理的数据。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @ApiOperation(value = "取消点赞帖子")
    @PostMapping("/unlike/{id}")
    public CommonResult<Void> unlike(@PathVariable("id") Long id) {
        return postApplicationService.unlike(id, getUserId());
    }

    /**
     * 将帖子加入当前用户收藏列表，供个人中心后续查看。
     * @param id 记录主键，用来定位本次要处理的数据。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @ApiOperation(value = "收藏帖子")
    @PostMapping("/favorite/{id}")
    public CommonResult<Void> favorite(@PathVariable("id") Long id) {
        return postApplicationService.favorite(id, getUserId());
    }

    /**
     * 从当前用户收藏列表移除帖子，并同步收藏状态。
     * @param id 记录主键，用来定位本次要处理的数据。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @ApiOperation(value = "取消收藏帖子")
    @PostMapping("/unFavorite/{id}")
    public CommonResult<Void> unFavorite(@PathVariable("id") Long id) {
        return postApplicationService.unFavorite(id, getUserId());
    }

    /**
     * 记录帖子转发次数，便于前台展示转发热度。
     * @param id 记录主键，用来定位本次要处理的数据。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @ApiOperation(value = "记录帖子转发")
    @PostMapping("/share/{id}")
    public CommonResult<Integer> sharePost(@PathVariable("id") Integer id) {
        return postApplicationService.sharePost(id);
    }

    /**
     * 根据用户行为和热度指标生成帖子推荐列表。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @ApiOperation(value = "个性化帖子推荐列表")
    @GetMapping("/recommend")
    public CommonResult<List<PostVo>> recommend() {
        return postApplicationService.recommend(getUserId());
    }

    /**
     * 分页查询帖子数据，返回列表内容和总数信息。
     * @param dto 请求 DTO，封装分页、筛选和排序条件。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @ApiOperation(value = "分页查询帖子列表")
    @PostMapping("/page")
    public CommonResult<PageResult<PostPageVo>> page(@RequestBody PostPageDTO dto) {
        return postApplicationService.page(dto);
    }

    /**
     * 按关键词检索帖子数据，给全局搜索或筛选框提供结果。
     * @param keyword 搜索关键词，支持按名称、内容或话题匹配。
     * @param gameId 游戏主键，用来定位被操作的游戏。
     * @param limit 返回数量上限，避免一次加载过多数据。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @ApiOperation(value = "全局搜索社区帖子")
    @GetMapping("/search")
    public CommonResult<List<PostVo>> search(@RequestParam(value = "keyword", required = false) String keyword,
                                             @RequestParam(value = "gameId", required = false) Integer gameId,
                                             @RequestParam(value = "limit", defaultValue = "20") Integer limit) {
        return postApplicationService.search(keyword, gameId, limit, getUserId());
    }

    /**
     * 统计近期高频话题，给搜索面板和社区热议区域展示。
     * @param limit 返回数量上限，避免一次加载过多数据。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @ApiOperation(value = "热门社区话题")
    @GetMapping("/hotTopics")
    public CommonResult<List<Map<String, Object>>> hotTopics(@RequestParam(defaultValue = "10") Integer limit) {
        return postApplicationService.hotTopics(limit);
    }

    /**
     * 审核帖子发布状态，决定内容是否可以出现在社区列表中。
     * @param id 记录主键，用来定位本次要处理的数据。
     * @param body 请求体数据，承载本次操作需要的业务字段。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @ApiOperation(value = "审核帖子")
    @PutMapping("/audit/{id}")
    public CommonResult<Void> auditPost(@PathVariable("id") Integer id, @RequestBody Map<String, String> body) {
        return postApplicationService.auditPost(id, body);
    }

    /**
     * 提交帖子审核拒绝后的申诉内容。
     * @param id 记录主键，用来定位本次要处理的数据。
     * @param body 请求体数据，承载本次操作需要的业务字段。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @ApiOperation(value = "用户申诉被拒绝的帖子")
    @PostMapping("/appeal/{id}")
    public CommonResult<Void> appealPost(@PathVariable("id") Integer id, @RequestBody Map<String, String> body) {
        return postApplicationService.appealPost(id, body, getUserId());
    }

    /**
     * 处理帖子申诉结果，并回写审核进度。
     * @param id 记录主键，用来定位本次要处理的数据。
     * @param body 请求体数据，承载本次操作需要的业务字段。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @ApiOperation(value = "处理帖子申诉")
    @PutMapping("/appeal/audit/{id}")
    public CommonResult<Void> auditAppeal(@PathVariable("id") Integer id, @RequestBody Map<String, String> body) {
        return postApplicationService.auditAppeal(id, body);
    }

    /**
     * 按游戏标签查询社区帖子，展示对应游戏下的讨论内容。
     * @param gameId 游戏主键，用来定位被操作的游戏。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @ApiOperation(value = "游戏下的动态帖子")
    @GetMapping("/game/{gameId}")
    public CommonResult<List<PostVo>> gamePost(@PathVariable("gameId") Integer gameId) {
        return postApplicationService.gamePost(gameId, getUserId());
    }

    /**
     * 查询指定用户公开的帖子动态。
     * @param userId 用户主键，用来限定当前操作的数据归属。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @ApiOperation(value = "用户下的动态帖子")
    @GetMapping("/user/{userId}")
    public CommonResult<List<PostVo>> userPost(@PathVariable("userId") Integer userId) {
        return postApplicationService.userPost(userId, getUserId());
    }

    /**
     * 查询当前用户发布的帖子，供我的帖子页面管理。
     * @param title title 字段，来源于当前接口入参或内部调用上下文。
     * @param auditStatus auditStatus 字段，来源于当前接口入参或内部调用上下文。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @ApiOperation(value = "我的帖子管理列表")
    @GetMapping("/my")
    public CommonResult<List<PostVo>> myPosts(@RequestParam(value = "title", required = false) String title,
                                              @RequestParam(value = "auditStatus", required = false) String auditStatus) {
        return postApplicationService.myPosts(title, auditStatus, getUserId());
    }

    /**
     * 按游戏主键读取关联帖子，支撑游戏详情页社区讨论区。
     * @param gameId 游戏主键，用来定位被操作的游戏。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @ApiOperation(value = "根据游戏ID获取相关帖子列表")
    @GetMapping("/posts/{gameId}")
    public CommonResult<List<PostVo>> getPostsByGameId(@PathVariable("gameId") Long gameId) {
        return postApplicationService.getPostsByGameId(gameId, getUserId());
    }

    /**
     * 读取帖子的 PostsByGameName 数据，供页面展示或后续业务判断。
     * @param name name 字段，来源于当前接口入参或内部调用上下文。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @ApiOperation(value = "根据游戏获取相关帖子列表")
    @GetMapping("/postsName")
    public CommonResult<List<PostVo>> getPostsByGameName(@RequestParam("name") String name) {
        return postApplicationService.getPostsByGameName(name, getUserId());
    }

    /**
     * 读取帖子的 Favorites 数据，供页面展示或后续业务判断。
     * @param id 记录主键，用来定位本次要处理的数据。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @ApiOperation(value = "获取用户收藏的帖子列表")
    @GetMapping("/favorites/{id}")
    public CommonResult<List<PostVo>> getFavorites(@PathVariable("id") Integer id) {
        return postApplicationService.getFavorites(id);
    }

    /**
     * 判断帖子当前状态是否满足业务条件。
     * @return true 表示帖子当前状态满足业务判断。
     */
    private boolean isAdminOrModerator() {
        UserInfo userInfo = this.getUserInfo();
        Integer roleId = userInfo.getRoleId();
        return Integer.valueOf(1).equals(roleId)
                || Integer.valueOf(3).equals(roleId)
                || (userInfo.getPermissions() != null && userInfo.getPermissions().contains("moderator"));
    }
}
