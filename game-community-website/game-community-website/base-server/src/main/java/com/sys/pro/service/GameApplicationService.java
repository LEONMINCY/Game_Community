package com.sys.pro.service;

import com.sys.pro.common.PageResult;
import com.sys.pro.pojo.Game;
import com.sys.pro.vo.GameVo;

import java.util.List;
import java.util.Map;

/**
 * 游戏中心应用服务。
 */
public interface GameApplicationService {
    /**
     * 根据主键读取游戏详情，供详情页、弹窗或后台审核场景使用。
     * @param id 记录主键，用来定位本次要处理的数据。
     * @param userId 用户主键，用来限定当前操作的数据归属。
     * @return 游戏在该步骤产出的业务结果。
     */

    GameVo getById(Integer id, Integer userId);
    /**
     * 接收新增游戏数据，完成入口校验后交由业务层保存。
     * @param game game 字段，来源于当前接口入参或内部调用上下文。
     */

    void create(Game game);
    /**
     * 按主键删除游戏记录，并让业务层同步处理关联状态。
     * @param id 记录主键，用来定位本次要处理的数据。
     */

    void delete(Long id);
    /**
     * 保存游戏编辑后的内容，让前台展示和后台管理保持一致。
     * @param game game 字段，来源于当前接口入参或内部调用上下文。
     */

    void update(Game game);
    /**
     * 读取全部可用游戏数据，供下拉框或初始化页面使用。
     * @param keyword 搜索关键词，支持按名称、内容或话题匹配。
     * @param limit 返回数量上限，避免一次加载过多数据。
     * @return 游戏列表数据。
     */

    List<Game> listAll(String keyword, Integer limit);
    /**
     * 读取游戏列表数据，按页面传入条件完成筛选和排序。
     * @param game game 字段，来源于当前接口入参或内部调用上下文。
     * @return 游戏分页结果，包含当前页数据和总数。
     */

    PageResult<Game> list(GameVo game);
    /**
     * 根据用户行为和热度指标生成游戏推荐列表。
     * @param userId 用户主键，用来限定当前操作的数据归属。
     * @return 游戏列表数据。
     */

    List<Game> recommend(Integer userId);
    /**
     * 按销量、好评率或业务热度读取热门游戏列表。
     * @return 游戏列表数据。
     */

    List<Game> listHot();
    /**
     * 按发帖量统计热门社区，供首页侧栏跳转到对应帖子列表。
     * @param limit 返回数量上限，避免一次加载过多数据。
     * @return 游戏列表数据。
     */

    List<Game> hotCommunities(Integer limit);
    /**
     * 读取游戏类型标签，供前台筛选和后台表单复用。
     * @return 游戏列表数据。
     */

    List<String> types();
    /**
     * 读取正在打折的游戏列表，展示限时优惠入口。
     * @return 游戏列表数据。
     */

    List<Game> discounts();
    /**
     * 汇总游戏详情页需要的评分、销量和趋势统计。
     * @param id 记录主键，用来定位本次要处理的数据。
     * @return 游戏聚合数据，键名与前端展示字段保持一致。
     */

    Map<String, Object> stats(Integer id);
}
