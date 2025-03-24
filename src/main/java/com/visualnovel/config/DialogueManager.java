package com.visualnovel.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.visualnovel.VisualNovel;
import com.visualnovel.dialogue.Dialogue;
import com.visualnovel.network.ServerNetworkHandler;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * 对话管理器，负责加载和管理对话配置
 */
public class DialogueManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(DialogueManager.class);
    private final Map<String, Dialogue> dialogues;
    private final Map<UUID, DialogueState> playerStates;
    private final Set<String> completedDialogues;
    private final Gson gson;

    public DialogueManager() {
        this.dialogues = new HashMap<>();
        this.playerStates = new HashMap<>();
        this.completedDialogues = new HashSet<>();
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        createDefaultDialogues();
        loadDialogues();
    }

    /**
     * 创建默认对话配置示例
     */
    private void createDefaultDialogues() {
        try {
            // 确保配置目录存在
            Path configDir = Paths.get("config", "visualnovel");
            Files.createDirectories(configDir);

            // 检查是否存在示例对话文件
            Path examplePath = configDir.resolve("example_dialogue.json");
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
                Dialogue.Choice option1 = new Dialogue.Choice("如何配置对话", "config_info");
                Dialogue.Choice option2 = new Dialogue.Choice("结束对话", "end");

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
                String json = gson.toJson(example);
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
        Path configDir = Paths.get("config", "visualnovel");

        try {
            if (!Files.exists(configDir)) {
                Files.createDirectories(configDir);
            }

            Files.list(configDir)
                .filter(path -> path.toString().toLowerCase().endsWith(".json"))
                .forEach(path -> {
                    try (BufferedReader reader = Files.newBufferedReader(path)) {
                        Dialogue dialogue = gson.fromJson(reader, Dialogue.class);
                        String fileName = path.getFileName().toString();
                        dialogues.put(fileName.substring(0, fileName.lastIndexOf('.')), dialogue);
                        LOGGER.info("已加载对话脚本: " + fileName);
                    } catch (IOException e) {
                        LOGGER.error("加载对话文件失败: " + path.getFileName(), e);
                    }
                });

            LOGGER.info("已加载所有对话脚本");
        } catch (IOException e) {
            LOGGER.error("无法访问配置目录: " + configDir, e);
        }
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
    public void startDialogue(ServerPlayerEntity player, String dialogueId, Entity targetEntity) {
        if (isDialogueCompleted(dialogueId)) {
            return;
        }

        Dialogue dialogue = dialogues.get(dialogueId);
        if (dialogue == null) {
            return;
        }

        // 创建对话状态
        DialogueState state = new DialogueState(dialogueId, player, targetEntity);
        state.setCurrentNodeId("start");
        playerStates.put(player.getUuid(), state);

        // 获取起始节点
        Dialogue.DialogueNode startNode = dialogue.getNodes().get("start");
        if (startNode != null) {
            String processedText = processVariables(startNode.getText(), player);
            ServerNetworkHandler.sendDialogue(player, processedText, dialogue.getName(), startNode.getChoices());
        }

        // 禁用村民的AI
        if (targetEntity instanceof VillagerEntity) {
            VillagerEntity villager = (VillagerEntity) targetEntity;
            villager.setAiDisabled(true);
            villager.setInvulnerable(true);
        }
    }

    /**
     * 处理下一个对话节点
     */
    public void processNextNode(PlayerEntity player) {
        DialogueState state = playerStates.get(player.getUuid());
        if (state == null) {
            endDialogue(player);
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

        // 检查是否是玩家在说话
        boolean isPlayerSpeaking = processedText.contains("{player_name}");

        // 发送对话UI到客户端
        if (player instanceof ServerPlayerEntity) {
            ServerNetworkHandler.sendDialogue((ServerPlayerEntity) player, processedText, dialogue.getName(),
                    node.getChoices());
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
        }
    }

    /**
     * 处理选项选择
     */
    public void handleChoice(PlayerEntity player, int choiceIndex) {
        DialogueState state = playerStates.get(player.getUuid());
        if (state == null) {
            endDialogue(player);
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
        DialogueState state = playerStates.get(player.getUuid());
        if (state != null && state.getTargetEntity() instanceof VillagerEntity) {
            VillagerEntity villager = (VillagerEntity) state.getTargetEntity();
            villager.setAiDisabled(false);
            villager.setInvulnerable(false);
        }
        
        playerStates.remove(player.getUuid());
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
            try {
                Item item = Registries.ITEM.get(new Identifier(itemId));
                return player.getInventory().contains(item.getDefaultStack());
            } catch (Exception e) {
                LOGGER.error("检查物品条件失败: " + itemId, e);
                return false;
            }
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
        return playerStates.get(playerUuid);
    }

    /**
     * 检查对话是否已完成
     */
    public boolean hasCompletedDialogue(UUID playerUuid, String dialogueId) {
        return completedDialogues.contains(dialogueId);
    }

    public void processChoice(ServerPlayerEntity player, int choiceIndex) {
        DialogueState state = playerStates.get(player.getUuid());
        if (state == null) {
            return;
        }

        Dialogue.DialogueNode currentNode = dialogues.get(state.getDialogueId()).getNodes()
                .get(state.getCurrentNodeId());
        if (currentNode == null || currentNode.getChoices() == null || choiceIndex >= currentNode.getChoices().size()) {
            return;
        }

        Dialogue.Choice choice = currentNode.getChoices().get(choiceIndex);
        String nextNodeId = choice.getNextNodeId();

        if (nextNodeId != null && !nextNodeId.isEmpty()) {
            Dialogue.DialogueNode nextNode = dialogues.get(state.getDialogueId()).getNodes().get(nextNodeId);
            if (nextNode != null) {
                state.setCurrentNodeId(nextNodeId);
                String processedText = processVariables(nextNode.getText(), player);
                ServerNetworkHandler.sendDialogue(player, processedText, dialogues.get(state.getDialogueId()).getName(),
                        nextNode.getChoices());
            }
        } else {
            // 如果没有下一个节点，结束对话
            playerStates.remove(player.getUuid());
            ServerNetworkHandler.closeDialogue(player);
        }
    }

    public void continueDialogue(ServerPlayerEntity player) {
        DialogueState state = getPlayerDialogueState(player.getUuid());
        if (state == null) return;

        Dialogue dialogue = dialogues.get(state.getDialogueId());
        if (dialogue == null) return;

        Dialogue.DialogueNode currentNode = dialogue.getNodes().get(state.getCurrentNodeId());
        if (currentNode == null) return;

        // 检查是否有下一个节点
        if (currentNode.getNextNodeId() != null && !currentNode.getNextNodeId().isEmpty()) {
            // 继续到下一个节点
            state.setCurrentNodeId(currentNode.getNextNodeId());
            Dialogue.DialogueNode nextNode = dialogue.getNodes().get(currentNode.getNextNodeId());
            if (nextNode != null) {
                String processedText = processVariables(nextNode.getText(), player);
                ServerNetworkHandler.sendDialogue(player, processedText, dialogues.get(state.getDialogueId()).getName(),
                        nextNode.getChoices());
            }
        } else {
            // 对话结束
            recordDialogueCompleted(player.getUuid(), state.getDialogueId());
            endDialogue(player);
        }
    }

    private boolean isDialogueCompleted(String dialogueId) {
        return completedDialogues.contains(dialogueId);
    }
}