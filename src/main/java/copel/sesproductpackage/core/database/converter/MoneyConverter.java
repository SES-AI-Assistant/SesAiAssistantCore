package copel.sesproductpackage.core.database.converter;

import copel.sesproductpackage.core.unit.Money;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.math.BigDecimal;

/**
 * Money と BigDecimal を相互変換する JPA AttributeConverter.
 *
 * <p>Money を PostgreSQL の numeric/decimal カラムに保存する際に使用されます。
 *
 * @author Copel Co., Ltd.
 */
@Converter(autoApply = true)
public class MoneyConverter implements AttributeConverter<Money, BigDecimal> {

  /**
   * Money をデータベース用の BigDecimal に変換します.
   *
   * @param attribute Money オブジェクト
   * @return BigDecimal またはnull
   */
  @Override
  public BigDecimal convertToDatabaseColumn(Money attribute) {
    if (attribute == null || attribute.isEmpty()) {
      return null;
    }
    return attribute.getValue();
  }

  /**
   * データベースから取得した BigDecimal を Money に変換します.
   *
   * @param dbData データベースから取得した BigDecimal
   * @return Money またはnull
   */
  @Override
  public Money convertToEntityAttribute(BigDecimal dbData) {
    if (dbData == null) {
      return null;
    }
    return new Money(dbData);
  }
}
