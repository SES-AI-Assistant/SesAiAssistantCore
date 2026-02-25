package copel.sesproductpackage.core.database.base;

import copel.sesproductpackage.core.api.gpt.Transformer;
import copel.sesproductpackage.core.unit.OriginalDateTime;
import copel.sesproductpackage.core.unit.Vector;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * SES AIアシスタントのベクトルDB群の基底クラス.
 *
 * @author 鈴木一矢
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public abstract class SES_AI_T_EntityBase extends EntityBase {
  /** 送信元グループ / from_group */
  @Column(physicalName = "from_group", logicalName = "送信元グループ")
  protected String fromGroup;

  /** 送信者ID / from_id */
  @Column(physicalName = "from_id", logicalName = "送信者ID")
  protected String fromId;

  /** 送信者名 / from_name */
  @Column(physicalName = "from_name", logicalName = "送信者名")
  protected String fromName;

  /** OpenAIベクトルデータ / vector_data. */
  @Column(physicalName = "vector_data", logicalName = "OpenAIベクトルデータ")
  protected Vector vectorData;

  /** 有効期限 / ttl */
  @Column(physicalName = "ttl", logicalName = "有効期限")
  protected OriginalDateTime ttl;

  /** ユークリッド距離 / distance. */
  @Column(physicalName = "distance", logicalName = "ユークリッド距離")
  protected double distance;

  /**
   * このエンティティが持つ内容をエンベディングする.
   *
   * @param transformer GPT
   * @throws IOException
   * @throws RuntimeException
   */
  public void embedding(final Transformer transformer) throws IOException, RuntimeException {
    this.vectorData = new Vector(transformer);
    // 元データではなく要約をエンベディングする
    this.vectorData.setRawString(this.getContentSummary());
    this.vectorData.embedding();
  }

  /**
   * テーブル内にこのエンティティの持つ内容と類似したレコードがあるかどうを判定する.
   *
   * @param connection DBコネクション
   * @param similarityThreshold
   *     類似度基準値(0.0～1.0で指定する。文章の一致率を示す。例えば0.8であれば、80%以上一致する文章が存在しなければユニークであると判定)
   * @return 類似するレコードがなければtrue、あればfalse
   * @throws SQLException
   */
  public boolean uniqueCheck(final Connection connection, final double similarityThreshold)
      throws SQLException {
    PreparedStatement preparedStatement = connection.prepareStatement(getCheckSql());
    preparedStatement.setString(1, this.getRawContent());
    preparedStatement.setString(2, this.getRawContent());
    preparedStatement.setDouble(3, similarityThreshold);
    ResultSet resultSet = preparedStatement.executeQuery();
    if (resultSet.next()) {
      return resultSet.getInt(1) < 1;
    }
    return true;
  }

  /**
   * 原文を取得します.
   *
   * @return 原文
   */
  protected abstract String getRawContent();

  /**
   * 要約を取得します.
   *
   * @return 要約
   */
  protected abstract String getContentSummary();

  /**
   * 重複チェック用SQLを取得します.
   *
   * @return SQL
   */
  protected abstract String getCheckSql();

  /**
   * このレコードのユークリッド距離で比較する.
   *
   * @param o SES_AI_T_EntityBase
   * @return このレコードの方が小さければtrue、遠ければfalse
   */
  public int compareTo(SES_AI_T_EntityBase o) {
    return Double.compare(this.getDistance(), o.getDistance());
  }
}
