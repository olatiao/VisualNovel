package com.visualnovel.network;

import com.visualnovel.VisualNovel;
import com.visualnovel.dialogue.Dialogue;
import com.visualnovel.ui.DialogueScreen;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
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
    public static final Identifier DIALOGUE_PACKET_ID = new Identifier(VisualNovel.MOD_ID, "dialogue");
    public static final Identifier DIALOGUE_END_PACKET_ID = new Identifier(VisualNovel.MOD_ID, "dialogue_end");
    
    /**
     * 注册网络包处理器
     */
    public static void register() {
        // 注册对话包处理器
        ClientPlayNetworking.registerGlobalReceiver(DIALOGUE_PACKET_ID, (client, handler, buf, responseSender) -> {
            try {
                String text = buf.readString();
                String title = buf.readString();
                int choiceCount = buf.readInt();
                List<Dialogue.Choice> choices = new ArrayList<>();
                
                for (int i = 0; i < choiceCount; i++) {
                    String choiceText = buf.readString();
                    String nextNodeId = buf.readString();
                    choices.add(new Dialogue.Choice(choiceText, nextNodeId));
                }

                client.execute(() -> {
                    if (client.currentScreen instanceof DialogueScreen) {
                        ((DialogueScreen) client.currentScreen).updateDialogue(text, title, choices);
                    } else {
                        client.setScreen(new DialogueScreen());
                    }
                });
            } catch (Exception e) {
                LOGGER.error("处理对话包时出错", e);
            }
        });
        
        // 注册对话结束包处理器
        ClientPlayNetworking.registerGlobalReceiver(DIALOGUE_END_PACKET_ID, (client, handler, buf, responseSender) -> {
            try {
                client.execute(() -> {
                    if (client.currentScreen instanceof DialogueScreen) {
                        client.setScreen(null);
                    }
                });
            } catch (Exception e) {
                LOGGER.error("处理对话结束包时出错", e);
            }
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
    public static void sendDialogueContinue() {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeInt(-1); // 使用-1表示继续对话
        ClientPlayNetworking.send(DIALOGUE_PACKET_ID, buf);
    }

    /**
     * 发送对话结束请求到服务器
     */
    public static void sendDialogueEnd() {
        PacketByteBuf buf = PacketByteBufs.create();
        ClientPlayNetworking.send(DIALOGUE_END_PACKET_ID, buf);
    }
} 