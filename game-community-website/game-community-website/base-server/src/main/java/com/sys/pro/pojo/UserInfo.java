package com.sys.pro.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

/**
 * UserInfo 是用户资料实体，负责承载数据库记录与Java字段之间的映射。
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("t_user_info")
@ApiModel(value="UserInfo对象", description="个人信息表")
public class UserInfo implements Serializable {



    private static final long serialVersionUID = 1L;



    @ApiModelProperty(value = "主键")

    @TableId(value = "id", type = IdType.AUTO)

    private Integer id;



    @ApiModelProperty(value = "用户id")

    private Integer userId;



    @ApiModelProperty(value = "昵称")

    private String nickname;



    @ApiModelProperty(value = "头像")

    private String avatar;



    @ApiModelProperty(value = "手机号")

    private String phone;

    @ApiModelProperty(value = "邮箱")
    private String email;



    @ApiModelProperty(value = "性别")

    private String gender;



    @ApiModelProperty(value = "年龄")

    private Integer age;



    @ApiModelProperty(value = "地址")
    private String address;

    @ApiModelProperty(value = "个性签名")
    private String signature;

    private Boolean privacyProfile;

    private Boolean privacyFollow;

    private Boolean privacyFans;

    @ApiModelProperty(value = "经验值")

    private Integer ex1;



    @ApiModelProperty(value = "等级")

    private Integer ex2;



    @ApiModelProperty(value = "是否删除")

    private Boolean deleted;



    /**

     * 用户角色

     */

    @TableField(exist = false)

    private List<String> permissions;



    /**

     * 用户角色id

     */

    @TableField(exist = false)

    private Integer roleId;



    /**

     * 用户名

     */

    @TableField(exist = false)

    private String username;



    /**

     * 是否关注

     */

    @TableField(exist = false)

    private Boolean follow;



    /**

     * 关注数

     */

    @TableField(exist = false)

    @ApiModelProperty(value = "关注数")

    private Integer followCount;



    /**

     * 粉丝数

     */

    @TableField(exist = false)

    @ApiModelProperty(value = "粉丝数")

    private Integer fansCount;



    @TableField(exist = false)
    @ApiModelProperty(value = "收藏数")
    private Integer favoriteCount;

    @TableField(exist = false)
    @ApiModelProperty(value = "发布帖子被收藏数")
    private Integer postFavoriteCount;

    @TableField(exist = false)
    @ApiModelProperty(value = "发布帖子获赞数")
    private Integer postLikeCount;

    @TableField(exist = false)
    @ApiModelProperty(value = "发布帖子被收藏与获赞总数")
    private Integer favoriteLikeCount;

    @TableField(exist = false)
    @ApiModelProperty(value = "发布内容数")
    private Integer postCount;

    @TableField(exist = false)
    @ApiModelProperty(value = "今天是否已签到")
    private Boolean hasCheckedToday;

    /**
     * 判断用户资料当前状态是否满足业务条件。
     * @return true 表示用户资料当前状态满足业务判断。
     */
    public Boolean isFollow() {
        if (follow == null) {
            follow = false;
        }
        return follow;
    }
}
