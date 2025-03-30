package copel.sesproductpackage.core.api.gpt;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

/**
 * 【SES AIアシスタント】
 * GPTが生成した回答を持つクラス.
 *
 * @author 鈴木一矢
 *
 */
@Slf4j
public class GptAnswer {
    /**
     * YESを意味する単語.
     */
    private static final String[] YES_ARRAY = { "YES", "yes", "YES.", "yes.", "はい", "はい。" };
    /**
     * NOを意味する単語.
     */
    private static final String[] NO_ARRAY = { "NO", "no", "NO.", "no.", "いいえ", "いいえ。" };

    /**
     * 回答.
     */
    private String answer;

    /**
     * コンストラクタ.
     *
     * @param answer 回答
     */
    public GptAnswer(final String answer, Class<?> transformerClass) {
        this.answer = answer != null ? answer.trim() : null;
        log.info("【{}】Competion APIで{}文字の生成を実行しました", transformerClass.getSimpleName(), this.length());
    }

    /**
     * この回答がNULLまたは空文字であるかどうかを判定する.
     *
     * @return NULLまたは空文字であればtrue、それ以外はfalse
     */
    public boolean isEmpty() {
        return this.answer != null ? (this.answer.isBlank() || this.answer.isEmpty()) : true;
    }

    /**
     * この回答の文字数を返却します.
     *
     * @return 文字数
     */
    public int length() {
        return this.answer != null ? this.answer.length() : 0;
    }

    /**
     * この回答がYESを意味する文字列（YES_ARRAYと一致する文字列）かどうかを判定する.
     *
     * @return YESを意味すればtrue、それ以外はfalse
     */
    public boolean isYES() {
        for (final String word : YES_ARRAY) {
            if (word.equals(this.answer)) {
                return true;
            }
        }
        return false;
    }

    /**
     * この回答がNOを意味する文字列（YES_ARRAYと一致する文字列）かどうかを判定する.
     *
     * @return NOを意味すればtrue、それ以外はfalse
     */
    public boolean isNO() {
        for (final String word : NO_ARRAY) {
            if (word.equals(this.answer)) {
                return true;
            }
        }
        return false;
    }

    /**
     * この回答が英数字のみであるかどうかを判定する.
     *
     * @return 英数字のみであればtrue、それ以外はfalse
     */
    public boolean isAlphanumeric() {
        return this.answer != null && this.answer.matches("^[a-zA-Z0-9]+$");
    }

    /**
     * この回答が英数字記号のみであるかどうかを判定する.
     *
     * @return 英数字記法のみであればtrue、それ以外はfalse
     */
    public boolean isAlphanumericWithSymbols() {
        return this.answer != null && this.answer.matches("^[a-zA-Z0-9!@#$%^&*()_+=\\-\\[\\]{};':\"\\\\|,.<>/?`~]+$");
    }

    /**
     * この回答が日本語のみであるかどうかを判定する.
     *
     * @return 日本語のみであればtrue、それ以外はfalse
     */
    public boolean isJapaneseOnly() {
        return this.answer != null && this.answer.matches("^[ぁ-んァ-ヶ一-龯ー]+$");
    }

    /**
     * 引数の文字列と一致する回答であるかどうかを判定する.
     *
     * @param word 比較対象
     * @return 一致するまたはNULL同士であればtrue、それ以外はfalse
     */
    public boolean equals(final String word) {
        return word != null ? word.equals(this.answer) : word == this.answer;
    }

    /**
     * この回答を整数型で取得する.
     *
     * @return 整数型の値
     */
    public Integer asInt() {
        return Integer.parseInt(this.answer);
    }

    /**
     * この回答がJSON配列形式かどうかを判定します.
     *
     * @return JSON配列形式であればtrue、それ以外はfalse
     */
    public boolean isJsonArrayFormat() {
        Pattern pattern = Pattern.compile("\\[([^]]*)\\]");
        Matcher matcher = pattern.matcher(this.answer);
        return matcher.find();
    }

    /**
     * この回答を配列形式で取得します.
     *
     * @return 配列
     * @throws JsonProcessingException 
     * @throws JsonMappingException 
     */
    @SuppressWarnings("unchecked")
    public List<String> getAsList() throws JsonMappingException, JsonProcessingException {
        Pattern pattern = Pattern.compile("\\[([^]]*)\\]");
        Matcher matcher = pattern.matcher(this.answer);
        if (matcher.find()) {
            String arrayStr = matcher.group(0).trim();
            arrayStr = arrayStr.replace("\t", "\\t");
            arrayStr = arrayStr.replace("\n", "\\n");
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(arrayStr, List.class);
        } else {
            return null;
        }
    }

    /**
     * 回答を返却します.
     *
     * @return 回答
     */
    public String getAnswer() {
        return this.answer;
    }
}
