package com.visualnovel.ui;

import com.visualnovel.VisualNovel;
import com.visualnovel.dialogue.Dialogue;
import com.visualnovel.network.ClientNetworkHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.List;

/**
 * 对话界面
 */
public class DialogueScreen extends Screen {
    private static final Logger LOGGER = LoggerFactory.getLogger(DialogueScreen.class);
    private final TextRenderer textRenderer;
    private String dialogueText;
    private String dialogueTitle;
    private final List<ButtonWidget> choiceButtons;
    private ButtonWidget continueButton;
    private Entity leftEntity;
    private Entity rightEntity;
    private final MinecraftClient client;
    private static final int DIALOGUE_BOX_HEIGHT = 150;
    private static final int DIALOGUE_BOX_PADDING = 15;
    private static final int SCREEN_MARGIN = 50;

    public DialogueScreen() {
        super(Text.literal("对话"));
        this.textRenderer = MinecraftClient.getInstance().textRenderer;
        this.choiceButtons = new ArrayList<>();
        this.client = MinecraftClient.getInstance();
    }

    @Override
    protected void init() {
        super.init();
        createEntities();
        // 清除所有按钮
        clearButtons();
    }

    private void clearButtons() {
        // 清除选项按钮
        for (ButtonWidget button : choiceButtons) {
            remove(button);
        }
        choiceButtons.clear();
        
        // 清除继续按钮
        if (continueButton != null) {
            remove(continueButton);
            continueButton = null;
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) { // ESC键
            this.close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void close() {
        if (client != null && client.player != null) {
            ClientNetworkHandler.sendDialogueEnd();
        }
        client.setScreen(null);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);
        super.render(context, mouseX, mouseY, delta);

        // 渲染实体模型
        int leftEntityX = width / 4;
        int rightEntityX = width * 3 / 4;
        int entityY = height / 2; // 将实体放在屏幕中间
        int entitySize = 100;

        // 渲染左侧实体（玩家）
        if (leftEntity instanceof LivingEntity) {
            InventoryScreen.drawEntity(context, leftEntityX, entityY, entitySize, 0, 0, (LivingEntity) leftEntity);
        }

        // 渲染右侧实体（村民）
        if (rightEntity instanceof LivingEntity) {
            InventoryScreen.drawEntity(context, rightEntityX, entityY, entitySize, 0, 0, (LivingEntity) rightEntity);
        }

        // 渲染对话框背景
        int dialogueBoxY = height - DIALOGUE_BOX_HEIGHT - SCREEN_MARGIN;
        context.fill(SCREEN_MARGIN, dialogueBoxY, width - SCREEN_MARGIN, height - SCREEN_MARGIN, 0x80000000); // 半透明黑色背景

        // 渲染对话标题
        if (dialogueTitle != null) {
            context.drawTextWithShadow(textRenderer, dialogueTitle,
                    SCREEN_MARGIN + DIALOGUE_BOX_PADDING,
                    dialogueBoxY + DIALOGUE_BOX_PADDING,
                    0xFFFFFF);
        }

        // 渲染对话文本
        if (dialogueText != null) {
            int textY = dialogueBoxY + DIALOGUE_BOX_PADDING + 20;
            for (String line : dialogueText.split("\n")) {
                context.drawTextWithShadow(textRenderer, line,
                        SCREEN_MARGIN + DIALOGUE_BOX_PADDING,
                        textY,
                        0xFFFFFF);
                textY += 12;
            }
        }

        // 渲染选项按钮或继续按钮（互斥显示）
        if (!choiceButtons.isEmpty()) {
            for (ButtonWidget button : choiceButtons) {
                button.render(context, mouseX, mouseY, delta);
            }
        } else if (continueButton != null) {
            continueButton.render(context, mouseX, mouseY, delta);
        }
    }

    private void createEntities() {
        if (client.world == null)
            return;

        // 创建玩家实体
        leftEntity = client.player;
        if (leftEntity != null) {
            leftEntity.setCustomName(client.player.getName());
            leftEntity.setCustomNameVisible(true);
        }

        // 创建村民实体
        rightEntity = EntityType.VILLAGER.create(client.world);
        if (rightEntity != null) {
            rightEntity.setCustomName(Text.literal("村民"));
            rightEntity.setCustomNameVisible(true);
            if (rightEntity instanceof VillagerEntity) {
                VillagerEntity villager = (VillagerEntity) rightEntity;
                villager.setAiDisabled(true);
                villager.setInvulnerable(true);
            }
        }
    }

    public void updateDialogue(String text, String title, List<Dialogue.Choice> choices) {
        this.dialogueText = text;
        this.dialogueTitle = title;
        
        // 清除所有旧的按钮
        clearButtons();
        
        // 如果有选项，创建选项按钮
        if (choices != null && !choices.isEmpty()) {
            int buttonY = height - DIALOGUE_BOX_HEIGHT - SCREEN_MARGIN - 30;
            int buttonSpacing = 25; // 按钮之间的间距
            
            for (int i = 0; i < choices.size(); i++) {
                final int choiceIndex = i;
                Dialogue.Choice choice = choices.get(i);
                ButtonWidget button = ButtonWidget.builder(Text.literal(choice.getText()), b -> {
                    ClientNetworkHandler.sendChoiceSelection(choiceIndex);
                }).dimensions(width / 2 - 100, buttonY - (choices.size() - 1 - i) * buttonSpacing, 200, 20).build();
                choiceButtons.add(button);
                addDrawableChild(button);
            }
        } else {
            // 如果没有选项，创建继续按钮
            int buttonY = height - DIALOGUE_BOX_HEIGHT - SCREEN_MARGIN - 30;
            continueButton = ButtonWidget.builder(Text.literal("继续"), b -> {
                ClientNetworkHandler.sendDialogueContinue();
            }).dimensions(width / 2 - 100, buttonY, 200, 20).build();
            addDrawableChild(continueButton);
        }
    }
}