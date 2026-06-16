package com.sys.pro.controller;

import com.sys.pro.common.CommonResult;
import com.sys.pro.controller.common.BaseController;
import com.sys.pro.service.UserExperienceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
/**
 * 每日任务接口，返回任务进度并处理签到、发帖、评论和购买经验奖励。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/daily-task")
public class DailyTaskController extends BaseController {

    private final UserExperienceService userExperienceService;

    /**
     * 汇总当前用户每日任务进度和可获得经验。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @GetMapping("/summary")
    public CommonResult<Map<String, Object>> summary() {
        return CommonResult.success(userExperienceService.summary(getUserId()));
    }
}
