package com.visualnovel.network;

import com.visualnovel.VisualNovel;
import com.visualnovel.dialogue.Dialogue;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * 服务器端网络处理器
 */
public class ServerNetworkHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger("VisualNovel");

    /**
     * 注册网络处理器
     */
    public static void register() {
        // 注册对话处理器
        ServerPlayNetworking.registerGlobalReceiver(VisualNovel.DIALOGUE_PACKET_ID,
                (server, player, handler, buf, responseSender) -> {
                    int choiceIndex = buf.readInt();
                    server.execute(() -> {
                        if (choiceIndex == -1) {
                            // 继续对话
                            VisualNovel.DIALOGUE_MANAGER.continueDialogue(player);
                        } else {
                            // 处理选项选择
                            VisualNovel.DIALOGUE_MANAGER.handleChoice(player, choiceIndex);
                        }
                    });
                });

        // 注册对话结束处理器
        ServerPlayNetworking.registerGlobalReceiver(VisualNovel.DIALOGUE_END_PACKET_ID,
                (server, player, handler, buf, responseSender) -> {
                    server.execute(() -> {
                        VisualNovel.DIALOGUE_MANAGER.endDialogue(player);
                    });
                });
    }

    /**
     * 发送对话UI到客户端
     */
    public static void sendDialogue(ServerPlayerEntity player, String text, String title,
            List<Dialogue.Choice> choices) {
        try {
            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeString(text != null ? text : "");
            buf.writeString(title != null ? title : "");
            buf.writeInt(choices != null ? choices.size() : 0);
            if (choices != null) {
                for (Dialogue.Choice choice : choices) {
                    buf.writeString(choice.getText() != null ? choice.getText() : "");
                    buf.writeString(choice.getNextNodeId() != null ? choice.getNextNodeId() : "");
                }
            }
            ServerPlayNetworking.send(player, VisualNovel.DIALOGUE_PACKET_ID, buf);
        } catch (Exception e) {
            LOGGER.error("发送对话包时出错", e);
        }
    }

    /**
     * 关闭对话UI
     */
    public static void closeDialogue(ServerPlayerEntity player) {
        try {
            PacketByteBuf buf = PacketByteBufs.create();
            ServerPlayNetworking.send(player, VisualNovel.DIALOGUE_END_PACKET_ID, buf);
            LOGGER.info("发送对话结束包到玩家: " + player.getName().getString());
        } catch (Exception e) {
            LOGGER.error("发送对话结束包时出错", e);
        }
    }

    /**
     * 播放声音
     */
    public static void playSound(ServerPlayerEntity player, String soundId) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeString(soundId);
        ServerPlayNetworking.send(player, VisualNovel.SOUND_PACKET_ID, buf);
    }
}