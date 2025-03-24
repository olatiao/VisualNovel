package com.visualnovel.event;

import com.visualnovel.VisualNovel;
import com.visualnovel.config.DialogueState;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;

import java.util.HashMap;
import java.util.Map;

/**
 * 交互事件处理类，用于处理实体右键交互事件
 */
public class InteractionEvents {
    
    // 存储实体和对话ID的映射关系，可以通过配置文件加载
    private static final Map<String, String> ENTITY_DIALOGUE_MAPPING = new HashMap<>();
    
    // 按实体类型注册默认对话
    static {
        // 这里可以添加一些默认的实体类型和对话ID映射
        ENTITY_DIALOGUE_MAPPING.put("minecraft:villager", "example");
    }
    
    /**
     * 注册所有事件监听器
     */
    public static void register() {
        registerEntityInteraction();
    }
    
    /**
     * 注册实体交互事件
     */
    private static void registerEntityInteraction() {
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            // 只处理主手的交互
            if (hand != Hand.MAIN_HAND) {
                return ActionResult.PASS;
            }
            
            // 检查玩家是否已经在对话中
            DialogueState state = VisualNovel.DIALOGUE_MANAGER.getPlayerDialogueState(player.getUuid());
            if (state != null) {
                // 玩家已经在对话中，继续处理当前对话
                VisualNovel.DIALOGUE_MANAGER.processNextNode(player);
                return ActionResult.SUCCESS;
            }
            
            // 玩家不在对话中，尝试开始新对话
            String dialogueId = getDialogueIdForEntity(entity, player);
            if (dialogueId != null) {
                boolean started = VisualNovel.DIALOGUE_MANAGER.startDialogue(dialogueId, player, entity);
                return started ? ActionResult.SUCCESS : ActionResult.PASS;
            }
            
            return ActionResult.PASS;
        });
    }
    
    /**
     * 根据实体类型和玩家状态获取适合的对话ID
     */
    private static String getDialogueIdForEntity(Entity entity, PlayerEntity player) {
        // 首先检查特定实体ID是否有映射
        String entityId = entity.getEntityName();
        String dialogueId = ENTITY_DIALOGUE_MAPPING.get(entityId);
        
        if (dialogueId == null) {
            // 检查实体类型是否有映射
            String entityType = entity.getType().toString();
            dialogueId = ENTITY_DIALOGUE_MAPPING.get(entityType);
        }
        
        // 如果没有找到映射，尝试使用默认对话
        if (dialogueId == null && !ENTITY_DIALOGUE_MAPPING.isEmpty()) {
            // 使用第一个映射作为默认
            dialogueId = ENTITY_DIALOGUE_MAPPING.values().iterator().next();
        }
        
        // 如果找到了对话ID，检查是否有特定条件的对话
        if (dialogueId != null) {
            // 检查玩家是否已完成该对话，如果已完成可以尝试加载后续对话
            if (VisualNovel.DIALOGUE_MANAGER.hasCompletedDialogue(player.getUuid(), dialogueId)) {
                String followUpDialogueId = dialogueId + "_followup";
                if (VisualNovel.DIALOGUE_MANAGER.hasDialogue(followUpDialogueId)) {
                    return followUpDialogueId;
                }
            }
            
            // 检查玩家手持物品，可能有特殊对话
            String heldItemId = player.getMainHandStack().getItem().toString();
            String specialDialogueId = dialogueId + "_" + heldItemId;
            if (VisualNovel.DIALOGUE_MANAGER.hasDialogue(specialDialogueId)) {
                return specialDialogueId;
            }
        }
        
        return dialogueId;
    }
} 