package com.hw.fiveroom.room;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

//需要spring管理的一个组件,可以后续方便依赖注入
@Component
public class RoomManager {
    private final Map<String,Room> rooms =new ConcurrentHashMap<>();
//    添加房间
    public boolean addRoom(Room room){
        return rooms.putIfAbsent(room.getRoomId(),room) ==null;
    }
//    查找房间
    public Room getRoom(String roomId){
        return rooms.get(roomId);
    }
//    删除房间
    public void removeRoom(String roomId){
        rooms.remove(roomId);
    }
}
