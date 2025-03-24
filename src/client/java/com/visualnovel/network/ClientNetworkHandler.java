package com.visualnovel.network;

import com.visualnovel.dialogue.Dialogue;
import com.visualnovel.ui.DialogueScreen;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * 客户端网络处理器
 */
public class ClientNetworkHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger("VisualNovel");
    
    // 网络包ID
    public static final Identifier DIALOGUE_PACKET_ID = new Identifier("visualnovel", "dialogue");
    public static final Identifier SOUND_PACKET_ID = new Identifier("visualnovel", "sound");
    
    /**
     * 注册网络包处理器
     */
    public static void register() {
        // 注册对话包处理器
        ClientPlayNetworking.registerGlobalReceiver(DIALOGUE_PACKET_ID, (client, handler, buf, responseSender) -> {
            String text = buf.readString();
            String title = buf.readString();
            int choiceCount = buf.readInt();
            List<Dialogue.Choice> choices = new ArrayList<>();
            
            for (int i = 0; i < choiceCount; i++) {
                Dialogue.Choice choice = new Dialogue.Choice();
                choice.setText(buf.readString());
                choice.setNextNodeId(buf.readString());
                choices.add(choice);
            }
            
            client.execute(() -> {
                if (client.currentScreen instanceof DialogueScreen) {
                    DialogueScreen screen = (DialogueScreen) client.currentScreen;
                    screen.updateDialogue(text, title, choices);
                } else {
                    client.setScreen(new DialogueScreen(text, title, choices));
                }
            });
        });
        
        // 注册声音包处理器
        ClientPlayNetworking.registerGlobalReceiver(SOUND_PACKET_ID, (client, handler, buf, responseSender) -> {
            String soundId = buf.readString();
            client.execute(() -> {
                // TODO: 播放声音
            });
        });
    }
    
    /**
     * 发送选择到服务器
     */
    public static void sendChoiceSelection(int choiceIndex) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeInt(choiceIndex);
        ClientPlayNetworking.send(DIALOGUE_PACKET_ID, buf);
    }
    
    /**
     * 发送继续对话请求到服务器
     */
    public static void sendContinueDialogue() {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeInt(-1); // 使用-1表示继续对话
        ClientPlayNetworking.send(DIALOGUE_PACKET_ID, buf);
    }
} 