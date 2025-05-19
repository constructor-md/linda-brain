package com.awesome.lindabrain.model.dto;


import com.awesome.lindabrain.model.entity.Session;
import lombok.Data;

import java.util.Date;

@Data
public class SessionInfoDto {

    private String sessionId;
    private String title;

    public static SessionInfoDto transferDto(Session session) {
        SessionInfoDto sessionInfoDto = new SessionInfoDto();
        sessionInfoDto.setSessionId(String.valueOf(session.getId()));
        sessionInfoDto.setTitle(session.getTitle());
        return sessionInfoDto;
    }


}
