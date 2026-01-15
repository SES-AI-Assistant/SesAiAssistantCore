package copel.sesproductpackage.core.api.slack;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Slack Incoming Webhook 用のメッセージEntityクラス.
 *
 * 参考：
 * IncomingWebhook
 * https://docs.slack.dev/messaging/sending-messages-using-incoming-webhooks/?utm_source=chatgpt.com#advanced_message_formatting
 * Chat Post Message
 * https://docs.slack.dev/reference/methods/chat.postMessage/
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SlackWebhookMessageEntity {
    private String channel;
    private String text;
    @JsonProperty("thread_ts")
    private String threadTs;
    @Builder.Default
    private List<SlackMessageBlock> blocks = new ArrayList<SlackMessageBlock>();

    public void addBlock(final SlackMessageBlock block) {
        this.blocks.add(block);
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class SlackMessageBlock {
        private BlockType type;
        private String blockId;
        private TextObject text;
        private List<TextObject> fields;
        private ImageAccessory accessory;

        public static SlackMessageBlock 区切り線 = SlackMessageBlock.builder().type(BlockType.DIVIDER).build();

        public void addTextObject(final TextObject textObject) {
            if (this.fields == null) {
                this.fields = new ArrayList<TextObject>();
            }
            this.fields.add(textObject);
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class TextObject {
        private TextType type;
        private String text;

        public static TextObject コードスニペット(String text) {
            return TextObject.builder().type(TextType.MRKDWN).text("```" + text + "```").build();
        }
        public static String リンクテキスト(String displayText, String link) {
            return "<" + link + " | " + displayText + ">";
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ImageAccessory {
        private AccessoryType type;
        private String imageUrl;
        private String altText;
    }

    public enum BlockType {
        @JsonProperty("section") SECTION,
        @JsonProperty("divider") DIVIDER,
        @JsonProperty("header") HEADER,
        @JsonProperty("image") IMAGE,
        @JsonProperty("actions") ACTIONS,
        @JsonProperty("context") CONTEXT
    }

    public enum TextType {
        @JsonProperty("mrkdwn") MRKDWN,
        @JsonProperty("plain_text") PLAIN_TEXT
    }

    public enum AccessoryType {
        @JsonProperty("image") IMAGE,
        @JsonProperty("button") BUTTON,
        @JsonProperty("static_select") STATIC_SELECT,
        @JsonProperty("datepicker") DATEPICKER
    }

    /**
     * このEntityをJSON形式に変換します.
     *
     * @return JSON形式の文字列
     * @throws JsonProcessingException 
     */
    public String toJson() throws JsonProcessingException {
        return new ObjectMapper().writeValueAsString(this);
    }

    @Data
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
    public static class SlackApiResponse {
        private boolean ok;
        private String ts;
        private String error;
        private Message message;

        @Data
        @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
        private static class Message {
            private String text;
            private String user;
            private String botId;
            private String type;
            @JsonProperty("ts")
            private String ts;
            @JsonProperty("app_id")
            private String appId;
        }
    }
}
