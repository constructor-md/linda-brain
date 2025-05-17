package com.awesome.lindabrain.model.request;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class DeepSeekMessage {
    private String role;
    private String content;

    public static DeepSeekMessage create() {
        return new DeepSeekMessage();
    }
}
