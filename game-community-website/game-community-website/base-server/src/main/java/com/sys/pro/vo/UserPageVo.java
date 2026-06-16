package com.sys.pro.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonFormat;
/**
 * 用户主页展示对象，汇总昵称、等级、隐私、关注粉丝和内容互动统计。
 */
@Data

@ApiModel(description = "用户分页查询返回对象")
public class UserPageVo {



    @ApiModelProperty(value = "用户ID")

    private Integer id;



    @ApiModelProperty(value = "用户名")

    private String username;



    @ApiModelProperty(value = "账号状态")

    private Boolean enable;



    @ApiModelProperty(value = "用户昵称")

    private String nickname;



    @ApiModelProperty(value = "手机号")

    private String phone;



    @ApiModelProperty(value = "头像")
    private String avatar;

    @ApiModelProperty(value = "角色ID")
    private Long roleId;

    @ApiModelProperty(value = "角色名称")
    private String roleName;

    @ApiModelProperty(value = "用户等级")
    private Integer userLevel;


    @ApiModelProperty(value = "经验值")

    private Integer ex1;



    @ApiModelProperty(value = "注册时间")

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")

    private LocalDateTime createdTime;



    @ApiModelProperty(value = "最后登录时间")

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")

    private LocalDateTime lastLogin;



    /**
     * 读取UserPageVo的 Ex1 数据，供页面展示或后续业务判断。
     * @return UserPageVo统计值或主键结果。
     */
    public Integer getEx1() {
        if (ex1 == null) {
            ex1 = 0;
        }
        return ex1;
    }
}
