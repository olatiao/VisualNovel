package com.visualnovel.network;

import com.visualnovel.VisualNovel;
import com.visualnovel.config.DialogueState;
import com.visualnovel.dialogue.Dialogue;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * 服务器端网络处理器
 */
public class ServerNetworkHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger("VisualNovel");
    
    // 显示对话数据包ID
    public static final Identifier SHOW_DIALOGUE_PACKET_ID = new Identifier("visualnovel", "show_dialogue");
    
    // 关闭对话数据包ID
    public static final Identifier CLOSE_DIALOGUE_PACKET_ID = new Identifier("visualnovel", "close_dialogue");
    
    // 播放声音数据包ID
    public static final Identifier PLAY_SOUND_PACKET_ID = new Identifier("visualnovel", "play_sound");
    
    // 继续对话数据包ID
    public static final Identifier CONTINUE_DIALOGUE_PACKET_ID = new Identifier("visualnovel", "continue_dialogue");
    
    // 选择选项数据包ID
    public static final Identifier CHOOSE_OPTION_PACKET_ID = new Identifier("visualnovel", "choose_option");
    
    // 设置说话者数据包ID
    public static final Identifier SET_SPEAKER_PACKET_ID = new Identifier("visualnovel", "set_speaker");
    
    /**
     * 注册网络处理器
     */
    public static void register() {
        // 注册继续对话处理器
        ServerPlayNetworking.registerGlobalReceiver(CONTINUE_DIALOGUE_PACKET_ID, (server, player, handler, buf, responseSender) -> {
            server.execute(() -> {
                VisualNovel.DIALOGUE_MANAGER.processNextNode(player);
            });
        });
        
        // 注册选择选项处理器
        ServerPlayNetworking.registerGlobalReceiver(CHOOSE_OPTION_PACKET_ID, (server, player, handler, buf, responseSender) -> {
            int choiceIndex = buf.readInt();
            server.execute(() -> {
                VisualNovel.DIALOGUE_MANAGER.handleChoice(player, choiceIndex);
            });
        });
    }
    
    /**
     * 发送对话UI到客户端
     */
    public static void sendDialogue(ServerPlayerEntity player, String text, String title, List<Dialogue.Choice> choices) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeString(text);
        buf.writeString(title);
        buf.writeInt(choices != null ? choices.size() : 0);
        if (choices != null) {
            for (Dialogue.Choice choice : choices) {
                buf.writeString(choice.getText());
                buf.writeString(choice.getNextNodeId() != null ? choice.getNextNodeId() : "");
            }
        }
        ServerPlayNetworking.send(player, VisualNovel.DIALOGUE_PACKET_ID, buf);
    }
    
    /**
     * 关闭对话UI
     */
    public static void closeDialogue(ServerPlayerEntity player) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeString("");
        buf.writeString("");
        buf.writeInt(0);
        ServerPlayNetworking.send(player, VisualNovel.DIALOGUE_PACKET_ID, buf);
    }
    
    /**
     * 播放声音
     */
    public static void playSound(ServerPlayerEntity player, String soundId) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeString(soundId);
        ServerPlayNetworking.send(player, VisualNovel.SOUND_PACKET_ID, buf);
    }
    
    /**
     * 设置当前说话者
     */
    public static void setSpeaker(ServerPlayerEntity player, boolean isPlayerSpeaking) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeBoolean(isPlayerSpeaking);
        ServerPlayNetworking.send(player, VisualNovel.DIALOGUE_PACKET_ID, buf);
    }
    
    /**
     * 处理客户端发送的对话包
     */
    public static void handleDialoguePacket(MinecraftServer server, ServerPlayerEntity player, ServerPlayNetworkHandler handler, PacketByteBuf buf, PacketSender responseSender) {
        // 处理客户端的选择
        int choiceIndex = buf.readInt();
        server.execute(() -> {
            VisualNovel.DIALOGUE_MANAGER.handleChoice(player, choiceIndex);
        });
    }
    
    /**
     * 处理客户端发送的声音包
     */
    public static void handleSoundPacket(MinecraftServer server, ServerPlayerEntity player, ServerPlayNetworkHandler handler, PacketByteBuf buf, PacketSender responseSender) {
        // 目前不需要处理客户端发送的声音包
    }
} 