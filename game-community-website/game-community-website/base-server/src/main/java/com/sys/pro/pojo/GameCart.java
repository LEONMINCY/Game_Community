package com.sys.pro.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 游戏购物车实体，对应 t_game_cart。
 */
@Data
@TableName("t_game_cart")
public class GameCart implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 购物车记录主键。
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID。
     */
    private Integer userId;

    /**
     * 游戏ID。
     */
    private Integer gameId;

    /**
     * 加入购物车时间。
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    /**
     * 逻辑删除标记。
     */
    private Boolean deleted;
}
