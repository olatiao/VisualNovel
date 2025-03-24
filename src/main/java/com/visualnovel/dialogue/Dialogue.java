package com.visualnovel.dialogue;

import java.util.List;
import java.util.Map;

/**
 * 对话类，用于存储对话配置
 */
public class Dialogue {
    private String id;
    private String name;
    private Map<String, DialogueNode> nodes;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String, DialogueNode> getNodes() {
        return nodes;
    }

    public void setNodes(Map<String, DialogueNode> nodes) {
        this.nodes = nodes;
    }

    /**
     * 对话节点类
     */
    public static class DialogueNode {
        private String id;
        private String text;
        private String sound;
        private String nextNodeId;
        private String condition;
        private String fallbackNodeId;
        private List<Choice> choices;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

        public String getSound() {
            return sound;
        }

        public void setSound(String sound) {
            this.sound = sound;
        }

        public String getNextNodeId() {
            return nextNodeId;
        }

        public void setNextNodeId(String nextNodeId) {
            this.nextNodeId = nextNodeId;
        }

        public String getCondition() {
            return condition;
        }

        public void setCondition(String condition) {
            this.condition = condition;
        }

        public String getFallbackNodeId() {
            return fallbackNodeId;
        }

        public void setFallbackNodeId(String fallbackNodeId) {
            this.fallbackNodeId = fallbackNodeId;
        }

        public List<Choice> getChoices() {
            return choices;
        }

        public void setChoices(List<Choice> choices) {
            this.choices = choices;
        }
    }

    /**
     * 对话选项类
     */
    public static class Choice {
        private String text;
        private String nextNodeId;

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

        public String getNextNodeId() {
            return nextNodeId;
        }

        public void setNextNodeId(String nextNodeId) {
            this.nextNodeId = nextNodeId;
        }
    }
}