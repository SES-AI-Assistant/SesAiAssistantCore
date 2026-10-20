package copel.sesproductpackage.core.database;

import copel.sesproductpackage.core.database.SES_AI_API_USAGE_HISTORY.Provider;
import copel.sesproductpackage.core.database.base.DynamoDbLot;
import copel.sesproductpackage.core.util.Properties;
import copel.sesproductpackage.core.util.SsmParameterKey;
import java.math.BigDecimal;

/** SES_AI_API_USAGE_HISTORY のリストと検索を管理する Lot クラスです。 */
public class SES_AI_API_USAGE_HISTORYLot extends DynamoDbLot<SES_AI_API_USAGE_HISTORY> {
  /** SES_AI_API_USAGE_HISTORY の Lot を生成します。 */
  public SES_AI_API_USAGE_HISTORYLot() {
    super(
        Properties.get(SsmParameterKey.API_USAGE_HISTORY_TABLE_NAME.getKey()),
        SES_AI_API_USAGE_HISTORY.class);
  }

  /**
   * SES_AI_API_USAGE_HISTORYテーブルをusageMonthで検索する.
   *
   * @param yyyy yyyy形式の年
   * @param mm MM形式の月
   */
  public void fetchByUsageMonth(final String yyyy, final String mm) {
    this.fetchByUsageMonth(yyyy + mm);
  }

  /**
   * SES_AI_API_USAGE_HISTORYテーブルをusageMonthで検索する.
   *
   * @param yyyyMm yyyyMM形式の日時
   */
  public void fetchByUsageMonth(final String yyyyMm) {
    this.fetchByColumn("usageMonth", yyyyMm);
  }

  /**
   * SES_AI_API_USAGE_HISTORYテーブルをprovider名で検索する.
   *
   * @param provider プロバイダ
   */
  public void fetchByProvider(final Provider provider) {
    this.fetchByColumn("provider", provider.name());
  }

  /**
   * このLotが持つEntityそれぞれがもつ入力文字数を合計して返却します.
   *
   * @return 合計入力文字数
   */
  public BigDecimal getSumInputCount() {
    return this.entityLot.stream()
        .map(SES_AI_API_USAGE_HISTORY::getInputCount)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  /**
   * このLotが持つEntityそれぞれがもつ出力文字数を合計して返却します.
   *
   * @return 合計出力文字数
   */
  public BigDecimal getSumOutputCount() {
    return this.entityLot.stream()
        .map(SES_AI_API_USAGE_HISTORY::getOutputCount)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }
}
