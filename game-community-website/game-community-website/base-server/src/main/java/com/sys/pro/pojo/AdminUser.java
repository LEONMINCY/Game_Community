package com.sys.pro.pojo;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.EqualsAndHashCode;

/**
 * Administrator account entity, mapped one-to-one to t_admin_user.
 */
@EqualsAndHashCode(callSuper = true)
@TableName("t_admin_user")
public class AdminUser extends BaseAccount {
}
