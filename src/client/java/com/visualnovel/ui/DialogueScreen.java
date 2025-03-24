package com.visualnovel.ui;

import com.visualnovel.dialogue.Dialogue;
import com.visualnovel.network.ClientNetworkHandler;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * 对话界面
 */
public class DialogueScreen extends Screen {
    private static final Logger LOGGER = LoggerFactory.getLogger("VisualNovel");

    private String text;
    private String title;
    private List<Dialogue.Choice> choices;
    private List<ButtonWidget> choiceButtons;

    public DialogueScreen(String text, String title, List<Dialogue.Choice> choices) {
        super(Text.empty());
        this.text = text;
        this.title = title;
        this.choices = choices;
        this.choiceButtons = new ArrayList<>();
    }

    @Override
    protected void init() {
        super.init();

        // 清除旧的按钮
        choiceButtons.forEach(this::remove);
        choiceButtons.clear();

        // 添加新的选项按钮
        if (choices != null && !choices.isEmpty()) {
            int buttonWidth = 200;
            int buttonHeight = 20;
            int buttonSpacing = 5;
            int startY = height - 100;

            for (int i = 0; i < choices.size(); i++) {
                final int choiceIndex = i;
                Dialogue.Choice choice = choices.get(i);
                ButtonWidget button = ButtonWidget.builder(Text.literal(choice.getText()), (b) -> {
                    ClientNetworkHandler.sendChoiceSelection(choiceIndex);
                })
                        .dimensions((width - buttonWidth) / 2, startY + i * (buttonHeight + buttonSpacing), buttonWidth,
                                buttonHeight)
                        .build();

                addDrawableChild(button);
                choiceButtons.add(button);
            }
        } else {
            // 如果没有选项，添加继续按钮
            ButtonWidget continueButton = ButtonWidget.builder(Text.literal("继续"), (b) -> {
                ClientNetworkHandler.sendContinueDialogue();
            })
                    .dimensions((width - 200) / 2, height - 50, 200, 20)
                    .build();

            addDrawableChild(continueButton);
            choiceButtons.add(continueButton);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);

        // 绘制对话框背景
        int boxWidth = width - 100;
        int boxHeight = 150;
        int boxX = (width - boxWidth) / 2;
        int boxY = height - boxHeight - 20;

        context.fill(boxX, boxY, boxX + boxWidth, boxY + boxHeight, 0x80000000);

        // 绘制标题
        if (title != null && !title.isEmpty()) {
            context.drawTextWithShadow(textRenderer, Text.literal(title), boxX + 10, boxY + 10, 0xFFFFFF);
        }

        // 绘制对话文本
        if (text != null && !text.isEmpty()) {
            context.drawTextWithShadow(textRenderer, Text.literal(text), boxX + 10, boxY + 30, 0xFFFFFF);
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    /**
     * 更新对话内容
     */
    public void updateDialogue(String text, String title, List<Dialogue.Choice> choices) {
        this.text = text;
        this.title = title;
        this.choices = choices;
        init();
    }
}