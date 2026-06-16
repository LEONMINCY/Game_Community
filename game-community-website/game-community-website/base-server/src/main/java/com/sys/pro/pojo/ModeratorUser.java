package com.sys.pro.pojo;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.EqualsAndHashCode;

/**
 * Community moderator account entity, mapped one-to-one to t_moderator_user.
 */
@EqualsAndHashCode(callSuper = true)
@TableName("t_moderator_user")
public class ModeratorUser extends BaseAccount {
}
