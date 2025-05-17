package com.awesome.lindabrain.model.request;

import lombok.Data;

@Data
public class ChatRequest {

    private String sessionId;
    private String content;

}
