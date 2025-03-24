package com.visualnovel.config;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;

/**
 * 对话状态类，用于存储玩家当前对话的状态
 */
public class DialogueState {
    private final String dialogueId;
    private final PlayerEntity player;
    private String currentNodeId;
    private final Entity targetEntity;
    
    public DialogueState(String dialogueId, PlayerEntity player, Entity targetEntity) {
        this.dialogueId = dialogueId;
        this.player = player;
        this.targetEntity = targetEntity;
        this.currentNodeId = "start";
    }
    
    public String getDialogueId() {
        return dialogueId;
    }
    
    public PlayerEntity getPlayer() {
        return player;
    }
    
    public String getCurrentNodeId() {
        return currentNodeId;
    }
    
    public void setCurrentNodeId(String currentNodeId) {
        this.currentNodeId = currentNodeId;
    }
    
    public Entity getTargetEntity() {
        return targetEntity;
    }
} 