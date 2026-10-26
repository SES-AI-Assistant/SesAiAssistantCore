package copel.sesproductpackage.core.unit;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.Objects;

/**
 * 情報種別Enum（案件・要員・対象外）.
 *
 * <p>AI の分類結果と画面登録の info_type で共通に使う。情報種別ごとに必要となる権限も、ここで一箇所にまとめる。
 *
 * @author Copel Co., Ltd.
 */
public enum InformationType {
  /** 案件情報. */
  JOB,
  /** 要員情報. */
  PERSON,
  /** 対象外（不在通知、退職挨拶等）. */
  Unknown;

  /**
   * JSON文字列から InformationType を解決する.
   *
   * <p>前後の空白と大文字小文字の違いを許容し、解決できない値・null は {@link #Unknown} として扱う。
   * 例外を投げないことで、リクエストのデシリアライズ自体は成功させ、値の妥当性判定（不正なら400を返す等）を 呼び出し側の業務ロジックに委ねる。
   *
   * @param value JSON上の値
   * @return 対応する InformationType。解決できない場合は {@link #Unknown}
   */
  @JsonCreator
  public static InformationType fromJson(final String value) {
    // null は空文字として扱い、どの種別にも一致しないため Unknown を返す
    String trimmed = Objects.toString(value, "").trim();
    for (InformationType informationType : values()) {
      if (informationType.name().equalsIgnoreCase(trimmed)) {
        return informationType;
      }
    }
    return Unknown;
  }

  /**
   * 情報種別の登録・インポートに必要な権限を返す.
   *
   * <p>案件（JOB）は案件一覧の権限、それ以外（PERSON と Unknown）はマッチング一覧の権限とする。 対象外を要員と同じ権限で扱うのは、Backend
   * の登録・インポートで従来から使われてきた判定を変えないためである。
   *
   * @return 必要な権限。JOB は {@link Permission#VIEW_JOB_LIST}、それ以外は {@link
   *     Permission#VIEW_MATCHING_LIST}
   */
  public Permission requiredPermission() {
    return this == JOB ? Permission.VIEW_JOB_LIST : Permission.VIEW_MATCHING_LIST;
  }
}
