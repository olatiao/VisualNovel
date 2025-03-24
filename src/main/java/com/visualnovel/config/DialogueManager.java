package com.visualnovel.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.visualnovel.dialogue.Dialogue;
import com.visualnovel.network.ServerNetworkHandler;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * 对话管理器，负责加载和管理对话配置
 */
public class DialogueManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("VisualNovel");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    // 配置目录
    private static final Path CONFIG_DIR = FabricLoader.getInstance().getConfigDir().resolve("visualnovel");
    
    // 存储所有对话配置
    private final Map<String, Dialogue> dialogues = new HashMap<>();
    
    // 存储玩家当前的对话状态
    private final Map<UUID, DialogueState> activeDialogues = new HashMap<>();
    
    // 存储玩家已完成的对话
    private final Set<String> completedDialogues = new HashSet<>();
    
    public DialogueManager() {
        createDefaultDialogues();
    }
    
    /**
     * 创建默认对话配置示例
     */
    private void createDefaultDialogues() {
        try {
            // 确保配置目录存在
            Files.createDirectories(CONFIG_DIR);
            
            // 检查是否存在示例对话文件
            Path examplePath = CONFIG_DIR.resolve("example_dialogue.json");
            if (!Files.exists(examplePath)) {
                // 创建示例对话
                Dialogue example = new Dialogue();
                example.setId("example");
                example.setName("示例对话");
                
                // 添加对话节点
                Dialogue.DialogueNode startNode = new Dialogue.DialogueNode();
                startNode.setId("start");
                startNode.setText("你好，{player_name}！欢迎使用视觉小说引擎。\n你手中拿着的是 {held_item}。");
                startNode.setSound("visualnovel:dialogue/greeting");
                startNode.setNextNodeId("choice");
                example.getNodes().put("start", startNode);
                
                // 添加选择节点
                Dialogue.DialogueNode choiceNode = new Dialogue.DialogueNode();
                choiceNode.setId("choice");
                choiceNode.setText("你想了解什么？");
                choiceNode.setSound("visualnovel:dialogue/question");
                
                // 添加选项
                Dialogue.Choice option1 = new Dialogue.Choice();
                option1.setText("如何配置对话");
                option1.setNextNodeId("config_info");
                
                Dialogue.Choice option2 = new Dialogue.Choice();
                option2.setText("结束对话");
                option2.setNextNodeId("end");
                
                choiceNode.getChoices().add(option1);
                choiceNode.getChoices().add(option2);
                example.getNodes().put("choice", choiceNode);
                
                // 添加信息节点
                Dialogue.DialogueNode configNode = new Dialogue.DialogueNode();
                configNode.setId("config_info");
                configNode.setText("你可以在配置文件夹中创建JSON文件来定义对话。\n每个对话包含多个节点和选择分支。");
                configNode.setSound("visualnovel:dialogue/explanation");
                configNode.setNextNodeId("end");
                example.getNodes().put("config_info", configNode);
                
                // 添加结束节点
                Dialogue.DialogueNode endNode = new Dialogue.DialogueNode();
                endNode.setId("end");
                endNode.setText("感谢使用！再见。");
                endNode.setSound("visualnovel:dialogue/goodbye");
                example.getNodes().put("end", endNode);
                
                // 添加条件分支示例
                Dialogue.DialogueNode conditionalNode = new Dialogue.DialogueNode();
                conditionalNode.setId("conditional");
                conditionalNode.setText("这是一个条件分支的示例");
                conditionalNode.setCondition("has_item:minecraft:diamond");
                conditionalNode.setNextNodeId("has_diamond");
                conditionalNode.setFallbackNodeId("no_diamond");
                example.getNodes().put("conditional", conditionalNode);
                
                Dialogue.DialogueNode hasDiamondNode = new Dialogue.DialogueNode();
                hasDiamondNode.setId("has_diamond");
                hasDiamondNode.setText("你有钻石！真棒！");
                example.getNodes().put("has_diamond", hasDiamondNode);
                
                Dialogue.DialogueNode noDiamondNode = new Dialogue.DialogueNode();
                noDiamondNode.setId("no_diamond");
                noDiamondNode.setText("你没有钻石。去挖矿吧！");
                example.getNodes().put("no_diamond", noDiamondNode);
                
                // 保存示例对话到JSON文件
                String json = GSON.toJson(example);
                Files.writeString(examplePath, json);
                
                LOGGER.info("已创建示例对话文件: " + examplePath);
            }
        } catch (IOException e) {
            LOGGER.error("创建默认对话配置失败", e);
        }
    }
    
    /**
     * 加载所有对话配置
     */
    public void loadDialogues() {
        dialogues.clear();
        File configDir = CONFIG_DIR.toFile();
        
        if (!configDir.exists() && !configDir.mkdirs()) {
            LOGGER.error("无法创建配置目录: " + configDir);
            return;
        }
        
        File[] files = configDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".json"));
        if (files == null) {
            LOGGER.warn("无法列出配置目录中的文件: " + configDir);
            return;
        }
        
        for (File file : files) {
            try (FileReader reader = new FileReader(file)) {
                Dialogue dialogue = GSON.fromJson(reader, Dialogue.class);
                dialogues.put(FilenameUtils.removeExtension(file.getName()), dialogue);
                LOGGER.info("已加载对话脚本: " + FilenameUtils.removeExtension(file.getName()));
            } catch (IOException e) {
                LOGGER.error("加载对话文件失败: " + file.getName(), e);
            }
        }
        
        LOGGER.info("已加载所有对话脚本");
    }
    
    /**
     * 检查是否存在指定ID的对话
     */
    public boolean hasDialogue(String dialogueId) {
        return dialogues.containsKey(dialogueId);
    }
    
    /**
     * 开始对话
     */
    public boolean startDialogue(String dialogueId, PlayerEntity player, Entity target) {
        Dialogue dialogue = dialogues.get(dialogueId);
        if (dialogue == null) {
            LOGGER.warn("尝试开始不存在的对话: " + dialogueId);
            return false;
        }
        
        // 创建对话状态
        DialogueState state = new DialogueState(dialogueId, target);
        activeDialogues.put(player.getUuid(), state);
        
        // 处理第一个节点
        processNextNode(player);
        
        return true;
    }
    
    /**
     * 处理下一个对话节点
     */
    public void processNextNode(PlayerEntity player) {
        DialogueState state = activeDialogues.get(player.getUuid());
        if (state == null) {
            return;
        }
        
        Dialogue dialogue = dialogues.get(state.getDialogueId());
        if (dialogue == null) {
            endDialogue(player);
            return;
        }
        
        Dialogue.DialogueNode node = dialogue.getNodes().get(state.getCurrentNodeId());
        if (node == null) {
            endDialogue(player);
            return;
        }
        
        // 处理条件判断
        if (node.getCondition() != null && !evaluateCondition(node.getCondition(), player)) {
            if (node.getFallbackNodeId() != null) {
                state.setCurrentNodeId(node.getFallbackNodeId());
                processNextNode(player);
            } else {
                endDialogue(player);
            }
            return;
        }
        
        // 显示对话文本
        String processedText = processVariables(node.getText(), player);
        
        // 设置当前说话者
        // 这里可以根据对话内容判断说话者，暂时简单处理
        boolean isPlayerSpeaking = node.getText().contains("{player_name}");
        if (player instanceof ServerPlayerEntity) {
            ServerNetworkHandler.setSpeaker((ServerPlayerEntity) player, isPlayerSpeaking);
        }
        
        // 发送对话UI到客户端
        if (player instanceof ServerPlayerEntity) {
            ServerNetworkHandler.sendDialogue((ServerPlayerEntity) player, processedText, dialogue.getName(), node.getChoices());
        }
        
        // 播放声音
        if (node.getSound() != null && !node.getSound().isEmpty() && player instanceof ServerPlayerEntity) {
            ServerNetworkHandler.playSound((ServerPlayerEntity) player, node.getSound());
        }
        
        // 如果没有选项，自动进入下一节点
        if (node.getChoices().isEmpty()) {
            if (node.getNextNodeId() != null) {
                state.setCurrentNodeId(node.getNextNodeId());
                // 不立即处理下一节点，等待玩家点击继续
            } else {
                // 对话结束
                recordDialogueCompleted(player.getUuid(), state.getDialogueId());
                endDialogue(player);
            }
        } else {
            // 有选项，等待玩家选择
            // 选项已通过sendDialogue发送
        }
    }
    
    /**
     * 处理选项选择
     */
    public void handleChoice(PlayerEntity player, int choiceIndex) {
        DialogueState state = activeDialogues.get(player.getUuid());
        if (state == null) {
            return;
        }
        
        Dialogue dialogue = dialogues.get(state.getDialogueId());
        if (dialogue == null) {
            endDialogue(player);
            return;
        }
        
        Dialogue.DialogueNode node = dialogue.getNodes().get(state.getCurrentNodeId());
        if (node == null || node.getChoices().isEmpty() || choiceIndex < 0 || choiceIndex >= node.getChoices().size()) {
            endDialogue(player);
            return;
        }
        
        Dialogue.Choice choice = node.getChoices().get(choiceIndex);
        state.setCurrentNodeId(choice.getNextNodeId());
        processNextNode(player);
    }
    
    /**
     * 结束对话
     */
    public void endDialogue(PlayerEntity player) {
        activeDialogues.remove(player.getUuid());
        // 关闭对话UI
        if (player instanceof ServerPlayerEntity) {
            ServerNetworkHandler.closeDialogue((ServerPlayerEntity) player);
        }
    }
    
    /**
     * 处理变量替换
     */
    private String processVariables(String text, PlayerEntity player) {
        if (text == null) {
            return "";
        }
        
        // 替换玩家名称
        text = text.replace("{player_name}", player.getName().getString());
        
        // 替换手持物品
        text = text.replace("{held_item}", player.getMainHandStack().getItem().getName().getString());
        
        // TODO: 添加更多变量处理
        
        return text;
    }
    
    /**
     * 评估条件
     */
    private boolean evaluateCondition(String condition, PlayerEntity player) {
        if (condition == null || condition.isEmpty()) {
            return true;
        }
        
        // 检查对话历史
        if (condition.startsWith("dialogue_completed:")) {
            String dialogueId = condition.substring("dialogue_completed:".length());
            return completedDialogues.contains(dialogueId);
        }
        
        // 检查物品
        if (condition.startsWith("has_item:")) {
            String itemId = condition.substring("has_item:".length());
            Item item = Registries.ITEM.get(new Identifier(itemId));
            return player.getInventory().contains(item.getDefaultStack());
        }
        
        // TODO: 添加更多条件类型
        
        return false;
    }
    
    /**
     * 记录对话完成状态
     */
    private void recordDialogueCompleted(UUID playerUuid, String dialogueId) {
        completedDialogues.add(dialogueId);
    }
    
    /**
     * 获取玩家的对话状态
     */
    public DialogueState getPlayerDialogueState(UUID playerUuid) {
        return activeDialogues.get(playerUuid);
    }
    
    /**
     * 检查对话是否已完成
     */
    public boolean hasCompletedDialogue(UUID playerUuid, String dialogueId) {
        return completedDialogues.contains(dialogueId);
    }
} 