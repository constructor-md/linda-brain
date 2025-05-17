package com.awesome.lindabrain.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 
 * @TableName chat
 */
@TableName(value ="chat")
@Data
public class Chat implements Serializable {
    /**
     * 
     */
    @TableId(value = "id")
    private Long id;

    /**
     * 所属会话id
     */
    @TableField(value = "session_id")
    private Long sessionId;

    /**
     * 0 代表linda，其余是用户id
     */
    @TableField(value = "sender")
    private Long sender;

    /**
     * 消息主题
     */
    @TableField(value = "message")
    private String message;

    /**
     * 消息发送时间
     */
    @TableField(value = "create_time")
    private Date createTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}