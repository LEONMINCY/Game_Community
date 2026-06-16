package com.sys.pro.service;

import com.sys.pro.pojo.GameRating;
import com.sys.pro.common.PageResult;
import com.baomidou.mybatisplus.extension.service.IService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * GameRatingService 定义游戏评价业务能力，供控制器和其他服务组合调用。
 */
public interface GameRatingService extends IService<GameRating> {

    /**
     * 完成游戏评价中的 createRating 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param gameRating gameRating 字段，来源于当前接口入参或内部调用上下文。
     */
    void createRating(GameRating gameRating);

    /**
     * 完成游戏评价中的 deleteRating 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param id 记录主键，用来定位本次要处理的数据。
     */
    void deleteRating(Long id);

    /**
     * 完成游戏评价中的 updateRating 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param gameRating gameRating 字段，来源于当前接口入参或内部调用上下文。
     */
    void updateRating(GameRating gameRating);
    

    /**
     * 提交已购用户的游戏评价，包含评分、好评或差评态度。
     * @param userId 用户主键，用来限定当前操作的数据归属。
     * @param gameId 游戏主键，用来定位被操作的游戏。
     * @param rating rating 字段，来源于当前接口入参或内部调用上下文。
     * @param content content 字段，来源于当前接口入参或内部调用上下文。
     * @param recommend 评价筛选标记，用来区分好评、差评或全部评价。
     * @return true 表示游戏评价当前状态满足业务判断。
     */

    boolean rateGame(Integer userId, Integer gameId, Integer rating, String content, Boolean recommend);


    /**
     * 读取游戏评价的 UserGameRating 数据，供页面展示或后续业务判断。
     * @param userId 用户主键，用来限定当前操作的数据归属。
     * @param gameId 游戏主键，用来定位被操作的游戏。
     * @return 游戏评价在该步骤产出的业务结果。
     */

    GameRating getUserGameRating(Integer userId, Integer gameId);



    /**
     * 读取游戏评价的 GameAverageRating 数据，供页面展示或后续业务判断。
     * @param gameId 游戏主键，用来定位被操作的游戏。
     * @return 游戏评价在该步骤产出的业务结果。
     */

    double getGameAverageRating(Integer gameId);



    /**
     * 读取游戏评价的 GameRatingCount 数据，供页面展示或后续业务判断。
     * @param gameId 游戏主键，用来定位被操作的游戏。
     * @return 游戏评价统计值或主键结果。
     */

    int getGameRatingCount(Integer gameId);



    /**
     * 读取游戏评价的 GameRatingList 数据，供页面展示或后续业务判断。
     * @param gameId 游戏主键，用来定位被操作的游戏。
     * @return 游戏评价列表数据。
     */
    List<Map<String, Object>> getGameRatingList(Integer gameId);

    /**
     * 读取游戏评价的 GameRatingPage 数据，供页面展示或后续业务判断。
     * @param gameId 游戏主键，用来定位被操作的游戏。
     * @param pageNo 当前页码，用于区分不同分页缓存。
     * @param pageSize 每页数量，用于控制分页列表长度。
     * @param recommend 评价筛选标记，用来区分好评、差评或全部评价。
     * @param startTime 评价发布时间起点，空值表示不限制开始时间。
     * @param endTime 评价发布时间终点，空值表示不限制结束时间。
     * @return 游戏评价分页结果，包含当前页数据和总数。
     */
    PageResult<Map<String, Object>> getGameRatingPage(Integer gameId, Integer pageNo, Integer pageSize, Boolean recommend,
                                                      LocalDateTime startTime, LocalDateTime endTime);
}
