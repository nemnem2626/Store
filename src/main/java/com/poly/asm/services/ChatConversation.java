package com.poly.asm.services;

import java.util.Date;

import com.poly.asm.entitys.User;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ChatConversation {
    private User customer;
    private String lastMessage;
    private String lastSenderRole;
    private Date lastTime;
    private long unread;
}
