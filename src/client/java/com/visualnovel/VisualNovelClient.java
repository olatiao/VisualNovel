package com.visualnovel;

import com.visualnovel.network.ClientNetworkHandler;
import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 客户端入口类
 */
public class VisualNovelClient implements ClientModInitializer {
	private static final Logger LOGGER = LoggerFactory.getLogger("VisualNovel");
	
	@Override
	public void onInitializeClient() {
		LOGGER.info("初始化视觉小说引擎客户端");
		
		// 注册网络处理器
		ClientNetworkHandler.register();
	}
}