package com.sys.pro.pojo;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.EqualsAndHashCode;

/**
 * Player account entity, mapped one-to-one to t_player_user.
 */
@EqualsAndHashCode(callSuper = true)
@TableName("t_player_user")
public class PlayerUser extends BaseAccount {
}
