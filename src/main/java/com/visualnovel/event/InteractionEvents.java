package com.visualnovel.event;

import com.visualnovel.VisualNovel;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;

public class InteractionEvents {
    public static void register() {
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (entity instanceof VillagerEntity && player instanceof ServerPlayerEntity) {
                String dialogueId = "example"; // 这里可以根据需要设置不同的对话ID
                VisualNovel.DIALOGUE_MANAGER.startDialogue((ServerPlayerEntity) player, dialogueId, entity);
                return ActionResult.SUCCESS;
            }
            return ActionResult.PASS;
        });
    }
} 