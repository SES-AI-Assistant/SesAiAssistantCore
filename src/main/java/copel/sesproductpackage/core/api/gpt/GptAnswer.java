package copel.sesproductpackage.core.api.gpt;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;

/**
 * GPTが生成した回答を持つクラス.
 *
 * @author Copel Co., Ltd.
 */
@Slf4j
public class GptAnswer {
  /** 回答. */
  private String answer;

  /**
   * コンストラクタ.
   *
   * @param answer 回答
   */
  // 既存の呼び出し側との互換性を保つため transformerClass は受け取るが使用しない
  @SuppressWarnings("PMD.UnusedFormalParameter")
  public GptAnswer(final String answer, Class<?> transformerClass) {
    this.answer = answer != null ? answer.trim() : null;
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
   * この回答が英数字のみであるかどうかを判定する.
   *
   * @return 英数字のみであればtrue、それ以外はfalse
   */
  public boolean isAlphanumeric() {
    return this.answer != null && this.answer.matches("^[a-zA-Z0-9]+$");
  }

  /**
   * 引数の文字列と一致する回答であるかどうかを判定する.
   *
   * @param word 比較対象
   * @return 一致するまたはNULL同士であればtrue、それ以外はfalse
   */
  public boolean equals(final String word) {
    return Objects.equals(word, this.answer);
  }

  /**
   * この回答が指定された文字列を含むかどうかを判定する.
   *
   * @param str 検索対象の文字列
   * @return 含む場合true、含まない場合またはNULLはfalse
   */
  public boolean contains(final String str) {
    return this.answer != null && str != null && this.answer.contains(str);
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
   * この回答を配列形式で取得します.
   *
   * @return 配列
   * @throws JsonProcessingException JSON の変換に失敗した場合
   * @throws JsonMappingException 処理中に異常が発生した場合
   */
  @SuppressWarnings("unchecked")
  public List<String> getAsList() throws JsonMappingException, JsonProcessingException {
    // 回答が無い場合は、配列が無い場合と同じく空のリストを返す
    if (this.answer == null) {
      return new ArrayList<>();
    }
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
      List<String> result = new ArrayList<>();
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
