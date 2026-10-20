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
 * NectarのベクトルDB群の基底クラス.
 *
 * @author Copel Co., Ltd.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public abstract class SesAiTableEntityBase extends EntityBase {

  protected SesAiTableEntityBase(String tenantId) {
    super(tenantId);
  }

  /** 送信元グループ / from_group */
  @Column(physicalName = "from_group", logicalName = "送信元グループ")
  protected String fromGroup;

  /** 送信者ID / from_id */
  @Column(physicalName = "from_id", logicalName = "送信者ID")
  protected String fromId;

  /** 送信者名 / from_name */
  @Column(physicalName = "from_name", logicalName = "送信者名")
  protected String fromName;

  /** メール件名 / mail_subject */
  @Column(physicalName = "mail_subject", logicalName = "メール件名")
  protected String mailSubject;

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
   * @throws IOException 入出力（ファイル・ストリーム）の操作に失敗した場合
   * @throws RuntimeException 実行時の異常が発生した場合
   */
  public void embedding(final Transformer transformer) throws IOException, RuntimeException {
    String contentSummary = this.getContentSummary();
    if (contentSummary == null || contentSummary.isEmpty()) {
      throw new RuntimeException("エンベディング対象の要約内容が空です。");
    }

    this.vectorData = new Vector(transformer);
    // 元データではなく要約をエンベディングする
    this.vectorData.setRawString(contentSummary);
    this.vectorData.embedding();

    if (this.vectorData.getValue() == null || this.vectorData.getValue().length == 0) {
      throw new RuntimeException("エンベディング処理に失敗しました。エンベディングAPIが無効な結果を返却しました。");
    }
  }

  /**
   * テーブル内にこのエンティティの持つ内容と類似したレコードがあるかどうを判定する.
   *
   * @param connection DBコネクション
   * @param similarityThreshold
   *     類似度基準値(0.0～1.0で指定する。文章の一致率を示す。例えば0.8であれば、80%以上一致する文章が存在しなければユニークであると判定)
   * @return 類似するレコードがなければtrue、あればfalse
   * @throws SQLException DB 操作（SQL の実行・結果の読み取り）に失敗した場合
   */
  public boolean uniqueCheck(final Connection connection, final double similarityThreshold)
      throws SQLException {
    try (PreparedStatement preparedStatement = connection.prepareStatement(getCheckSql())) {
      preparedStatement.setString(1, this.tenantId);
      preparedStatement.setString(2, this.getRawContent());
      preparedStatement.setString(3, this.getRawContent());
      preparedStatement.setDouble(4, similarityThreshold);
      try (ResultSet resultSet = preparedStatement.executeQuery()) {
        if (resultSet.next()) {
          return resultSet.getInt(1) < 1;
        }
        return true;
      }
    }
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
   * @param o SesAiTableEntityBase
   * @return このレコードの方が小さければtrue、遠ければfalse
   */
  public int compareTo(SesAiTableEntityBase o) {
    return Double.compare(this.getDistance(), o.getDistance());
  }
}
