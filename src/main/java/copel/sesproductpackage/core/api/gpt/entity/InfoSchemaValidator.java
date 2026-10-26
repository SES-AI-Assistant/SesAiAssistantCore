package copel.sesproductpackage.core.api.gpt.entity;

import com.fasterxml.jackson.databind.util.StdConverter;
import copel.sesproductpackage.core.unit.InformationType;

/**
 * 読み込んだ個別情報の整合性を検査するコンバータ.
 *
 * <p>summary が無くても読み込めるのは種別が Unknown の場合だけにする。PERSON / JOB で summary が無い出力を
 * そのまま通すと、登録されないまま成功扱いになり要員・案件が失われるため、解析の失敗として扱う。
 *
 * @author Copel Co., Ltd.
 */
public class InfoSchemaValidator
    extends StdConverter<ContentParseSchema.InfoSchema, ContentParseSchema.InfoSchema> {
  @Override
  public ContentParseSchema.InfoSchema convert(final ContentParseSchema.InfoSchema value) {
    boolean needsSummary =
        value.getType() == InformationType.PERSON || value.getType() == InformationType.JOB;
    if (needsSummary && value.getSummary() == null) {
      throw new IllegalArgumentException("種別が " + value.getType() + " の情報に summary がありません");
    }
    return value;
  }
}
