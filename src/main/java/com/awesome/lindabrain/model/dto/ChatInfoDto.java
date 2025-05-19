package com.awesome.lindabrain.model.dto;

import com.awesome.lindabrain.commons.Constants;
import com.awesome.lindabrain.model.entity.Chat;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

@Data
public class ChatInfoDto {

    private String content;
    // Linda / User
    private String role;
    @JsonFormat(pattern = Constants.TIME_FORMATTER, timezone = "UTF+8")
    private Date createTime;

    public static ChatInfoDto transferDto(Chat chat) {
        ChatInfoDto chatInfoDto = new ChatInfoDto();
        chatInfoDto.setContent(chat.getMessage());
        chatInfoDto.setRole(chat.getSender() == 0 ? Constants.CHAT_SENDER_LINDA : Constants.CHAT_SENDER_USER);
        chatInfoDto.setCreateTime(chat.getCreateTime());
        return chatInfoDto;
    }

}
