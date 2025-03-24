package com.visualnovel.events;

import com.visualnovel.VisualNovel;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.util.ActionResult;

public class InteractionEvents {
    public static void register() {
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (entity instanceof VillagerEntity && player instanceof ServerPlayerEntity) {
                VillagerEntity villager = (VillagerEntity) entity;
                ServerPlayerEntity serverPlayer = (ServerPlayerEntity) player;
                // 开始对话
                VisualNovel.DIALOGUE_MANAGER.startDialogue(serverPlayer, "example", villager);
                return ActionResult.SUCCESS;
            }
            return ActionResult.PASS;
        });
    }
}