package com.visualnovel;

import com.visualnovel.config.DialogueManager;
import com.visualnovel.event.InteractionEvents;
import com.visualnovel.network.ServerNetworkHandler;
import net.fabricmc.api.ModInitializer;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 视觉小说引擎主类
 */
public class VisualNovel implements ModInitializer {
	public static final String MOD_ID = "visualnovel";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	
	// 网络包标识符
	public static final Identifier DIALOGUE_PACKET_ID = new Identifier(MOD_ID, "dialogue");
	public static final Identifier DIALOGUE_END_PACKET_ID = new Identifier(MOD_ID, "dialogue_end");
	public static final Identifier SOUND_PACKET_ID = new Identifier(MOD_ID, "sound");
	
	// 对话管理器
	public static final DialogueManager DIALOGUE_MANAGER = new DialogueManager();
	
	@Override
	public void onInitialize() {
		LOGGER.info("正在初始化视觉小说引擎...");
		
		// 加载对话配置
		DIALOGUE_MANAGER.loadDialogues();
		
		// 注册网络包处理器
		ServerNetworkHandler.register();
		
		// 注册交互事件
		InteractionEvents.register();
		
		LOGGER.info("视觉小说引擎初始化完成");
	}
}