package com.sys.pro.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 账号统一编号序列表实体，对应 t_account_sequence。
 */
@Data
@TableName("t_account_sequence")
public class AccountSequence implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 自增主键，用于为三类账号表生成不冲突的用户编号。
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;
}
