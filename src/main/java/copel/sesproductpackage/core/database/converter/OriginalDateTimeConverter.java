package copel.sesproductpackage.core.database.converter;

import copel.sesproductpackage.core.unit.OriginalDateTime;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.time.LocalDateTime;

/**
 * OriginalDateTime と LocalDateTime を相互変換する JPA AttributeConverter.
 *
 * <p>OriginalDateTime を PostgreSQL の timestamp カラムに保存する際に使用されます。
 *
 * @author Copel Co., Ltd.
 */
@Converter(autoApply = true)
public class OriginalDateTimeConverter
    implements AttributeConverter<OriginalDateTime, LocalDateTime> {

  /**
   * OriginalDateTime をデータベース用の LocalDateTime に変換します.
   *
   * @param attribute OriginalDateTime オブジェクト
   * @return LocalDateTime またはnull
   */
  @Override
  public LocalDateTime convertToDatabaseColumn(OriginalDateTime attribute) {
    if (attribute == null) {
      return null;
    }
    return attribute.toLocalDateTime();
  }

  /**
   * データベースから取得した LocalDateTime を OriginalDateTime に変換します.
   *
   * @param dbData データベースから取得した LocalDateTime
   * @return OriginalDateTime またはnull
   */
  @Override
  public OriginalDateTime convertToEntityAttribute(LocalDateTime dbData) {
    if (dbData == null) {
      return null;
    }
    return new OriginalDateTime(
        dbData.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
  }
}
