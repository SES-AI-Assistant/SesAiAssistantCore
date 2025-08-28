package copel.sesproductpackage.core.database;

import java.math.BigDecimal;

import copel.sesproductpackage.core.database.SES_AI_API_USAGE_HISTORY.Provider;
import copel.sesproductpackage.core.database.base.DynamoDBLot;

public class SES_AI_API_USAGE_HISTORYLot extends DynamoDBLot<SES_AI_API_USAGE_HISTORY> {
    SES_AI_API_USAGE_HISTORYLot() {
        super("SES_AI_API_USAGE_HISTORY", SES_AI_API_USAGE_HISTORY.class);
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
                .map(e -> e.getInputCount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * このLotが持つEntityそれぞれがもつ出力文字数を合計して返却します.
     *
     * @return 合計出力文字数
     */
    public BigDecimal getSumOutputCount() {
        return this.entityLot.stream()
                .map(e -> e.getOutputCount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
