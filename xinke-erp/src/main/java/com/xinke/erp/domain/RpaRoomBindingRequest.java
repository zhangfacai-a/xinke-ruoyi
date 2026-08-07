package com.xinke.erp.domain;

import java.util.List;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class RpaRoomBindingRequest
{
    @NotNull(message = "roomKeys不能为空")
    @Size(max = 100, message = "单次最多绑定100个直播间")
    private List<@Size(max = 128, message = "roomKey长度不能超过128") String> roomKeys;

    public List<String> getRoomKeys() { return roomKeys; }
    public void setRoomKeys(List<String> roomKeys) { this.roomKeys = roomKeys; }
}
