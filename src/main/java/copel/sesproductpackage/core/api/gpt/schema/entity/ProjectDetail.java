package copel.sesproductpackage.core.api.gpt.schema.entity;

import copel.sesproductpackage.core.unit.ProjectPeriod;
import java.util.Objects;

/**
 * 集計に使った案件1件の内訳を表すクラス. 要約文の「直近のPJ経験」の1行になる。
 *
 * @author Copel Co., Ltd.
 */
final class ProjectDetail {

  /** 案件期間。集計できた（開始と終了が有効な）期間だけを持つ。 */
  private final ProjectPeriod period;

  /** 案件の内容。未記載の場合は空文字にそろえる。 */
  private final String perspective;

  /**
   * コンストラクタ.
   *
   * @param period 案件期間
   * @param perspective 案件の内容。null の場合は空文字として扱う
   */
  ProjectDetail(final ProjectPeriod period, final String perspective) {
    this.period = period;
    this.perspective = Objects.requireNonNullElse(perspective, "");
  }

  /**
   * 案件期間を返します.
   *
   * @return 案件期間
   */
  ProjectPeriod period() {
    return this.period;
  }

  /**
   * 要約文の1行を返します.
   *
   * <p>「2024年4月-2024年9月: 基幹システムの改修」の形式にする。期間は開始と終了の年月の表記をそろえる。
   *
   * @return 要約文の1行
   */
  String toSummaryLine() {
    return this.period.start().toJapaneseFormat()
        + "-"
        + this.period.end().toJapaneseFormat()
        + ": "
        + this.perspective;
  }
}
