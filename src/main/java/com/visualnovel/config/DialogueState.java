package com.visualnovel.config;

import net.minecraft.entity.Entity;

/**
 * 对话状态类，用于存储玩家当前对话的状态
 */
public class DialogueState {
    private final String dialogueId;
    private String currentNodeId;
    private final Entity target;
    
    public DialogueState(String dialogueId, Entity target) {
        this.dialogueId = dialogueId;
        this.currentNodeId = "start";
        this.target = target;
    }
    
    public String getDialogueId() {
        return dialogueId;
    }
    
    public String getCurrentNodeId() {
        return currentNodeId;
    }
    
    public void setCurrentNodeId(String currentNodeId) {
        this.currentNodeId = currentNodeId;
    }
    
    public Entity getTarget() {
        return target;
    }
} 