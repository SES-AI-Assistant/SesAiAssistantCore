package copel.sesproductpackage.core.api.gpt;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;

/**
 * 【SES AIアシスタント】 GPTが生成した回答を持つクラス.
 *
 * @author Copel Co., Ltd.
 */
@Slf4j
public class GptAnswer {
  /** YESを意味する単語. */
  private static final String[] YES_ARRAY = {"ＹＥＳ", "YES", "yes", "YES.", "yes.", "はい", "はい。"};

  /** NOを意味する単語. */
  private static final String[] NO_ARRAY = {"ＮＯ", "NO", "no", "NO.", "no.", "いいえ", "いいえ。"};

  /** 回答. */
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
    return this.answer == null || this.answer.isBlank();
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
   * この回答がYESを意味する文字列（YES_ARRAYと一致する文字列）から始まる文字列かどうかを判定する.
   *
   * @return YESを意味すればtrue、それ以外はfalse
   */
  public boolean isYES() {
    for (final String word : YES_ARRAY) {
      if (word.startsWith(this.answer)) {
        return true;
      }
    }
    return false;
  }

  /**
   * この回答がNOを意味する文字列（NO_ARRAYと一致する文字列）から始まる文字列かどうかを判定する.
   *
   * @return NOを意味すればtrue、それ以外はfalse
   */
  public boolean isNO() {
    for (final String word : NO_ARRAY) {
      if (word.startsWith(this.answer)) {
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
    return this.answer != null
        && this.answer.matches("^[a-zA-Z0-9!@#$%^&*()_+=\\-\\[\\]{};':\"\\\\|,.<>/?`~]+$");
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
    // AIの出力するJSON配列内に `]` が含まれている場合（オブジェクトの配列など）や、
    // 改行が含まれている場合でも、途中で抽出が打ち切られないように Pattern.DOTALL を指定し、
    // 「最初の `[` から最後の `]` まで」を確実に取得できるように修正。
    Pattern pattern = Pattern.compile("\\[.*\\]", Pattern.DOTALL);
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
    // 改行やネストされた括弧が存在しても配列全体を抽出できるよう正規表現を強化。
    Pattern pattern = Pattern.compile("\\[.*\\]", Pattern.DOTALL);
    Matcher matcher = pattern.matcher(this.answer);
    if (matcher.find()) {
      String arrayStr = matcher.group(0).trim();
      ObjectMapper objectMapper = new ObjectMapper();
      List<?> parsedList;
      try {
        // JSON→Listにパース
        parsedList = objectMapper.readValue(arrayStr, List.class);
      } catch (JsonProcessingException e) {
        // そのままJSON→Listにパースできない場合は制御文字をエスケープ
        arrayStr = arrayStr.replace("\n", "\\n").replace("\t", "\\t").replace("\r", "\\r");
        parsedList = objectMapper.readValue(arrayStr, List.class);
      }
      
      // パースされた要素が必ずしもStringとは限らない（AIがJSONオブジェクトで返す場合がある）ため、
      // 盲目的に List<String> へキャストした際の ClassCastException 発生を防止。
      // 要素がStringでない場合は、再度JSON文字列に変換してリストに格納。
      java.util.List<String> result = new java.util.ArrayList<>();
      for (Object obj : parsedList) {
        if (obj instanceof String) {
          result.add((String) obj);
        } else {
          result.add(objectMapper.writeValueAsString(obj));
        }
      }
      return result;
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

  @Override
  public String toString() {
    return this.answer;
  }
}
