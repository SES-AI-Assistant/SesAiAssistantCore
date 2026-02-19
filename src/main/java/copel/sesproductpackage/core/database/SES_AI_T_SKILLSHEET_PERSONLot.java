package copel.sesproductpackage.core.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import copel.sesproductpackage.core.database.base.EntityLotBase;
import copel.sesproductpackage.core.unit.Vector;
import lombok.extern.slf4j.Slf4j;

/**
 * 【Entityクラス】
 * スキルシート情報と要員情報を結合したEntity(SES_AI_T_SKILLSHEET_PERSON)のLotクラス.
 *
 * @author
 *
 */
@Slf4j
public class SES_AI_T_SKILLSHEET_PERSONLot extends EntityLotBase<SES_AI_T_SKILLSHEET_PERSON> {

    /**
     * ベクトル検索SQL (SES_AI_T_SKILLSHEET主導).
     * コサイン類似度が指定値(k)以上、かつ上位指定件数.
     * 1 - (vector <=> query) >= k <-- コサイン類似度 >= k
     * vector <=> query はコサイン距離 (1 - 類似度) を返す
     * そのため、類似度 = 1 - 距離
     * 距離 <= 1 - k
     */
    private final static String RETRIEVE_SQL = "SELECT s.file_id, s.file_content_summary, s.vector_data, p.person_id, p.content_summary, s.vector_data <=> ?::vector AS distance FROM SES_AI_T_SKILLSHEET s INNER JOIN SES_AI_T_PERSON p ON s.file_id = p.file_id WHERE 1 - (s.vector_data <=> ?::vector) >= ? ORDER BY distance ASC LIMIT ?";

    /**
     * コンストラクタ.
     */
    public SES_AI_T_SKILLSHEET_PERSONLot() {
        super();
    }

    /**
     * ベクトル検索を実行し結果をこのLotに保持します(閾値デフォルト0, 上位5件).
     *
     * @param connection DBコネクション
     * @param query      検索ベクトル
     * @throws SQLException
     */
    public void retrieve(final Connection connection, final Vector query) throws SQLException {
        retrieve(connection, query, 0, 5);
    }

    /**
     * ベクトル検索を実行し結果をこのLotに保持します(閾値デフォルト0).
     *
     * @param connection DBコネクション
     * @param query      検索ベクトル
     * @param limit      取得上限件数
     * @throws SQLException
     */
    public void retrieve(final Connection connection, final Vector query, final int limit) throws SQLException {
        retrieve(connection, query, 0, limit);
    }

    /**
     * ベクトル検索を実行し結果をこのLotに保持します.
     *
     * @param connection          DBコネクション
     * @param query               検索ベクトル
     * @param similarityThreshold 類似度閾値 (0.0 ~ 1.0)
     * @param limit               取得上限件数
     * @throws SQLException
     */
    public void retrieve(final Connection connection, final Vector query,
            final double similarityThreshold, final int limit) throws SQLException {
        if (connection == null || query == null) {
            return;
        }

        PreparedStatement preparedStatement = connection.prepareStatement(RETRIEVE_SQL);
        String vectorStr = query.toString();
        preparedStatement.setString(1, vectorStr);
        preparedStatement.setString(2, vectorStr);
        preparedStatement.setDouble(3, similarityThreshold);
        preparedStatement.setInt(4, limit);

        ResultSet resultSet = preparedStatement.executeQuery();
        this.entityLot = new ArrayList<SES_AI_T_SKILLSHEET_PERSON>();

        while (resultSet.next()) {
            SES_AI_T_SKILLSHEET_PERSON entity = new SES_AI_T_SKILLSHEET_PERSON();

            // スキルシート情報を設定
            entity.setFileId(resultSet.getString("file_id"));
            entity.setFileContentSummary(resultSet.getString("file_content_summary"));

            // 要員情報を設定
            entity.setPersonId(resultSet.getString("person_id"));
            entity.setContentSummary(resultSet.getString("content_summary"));

            // 結合Entityに距離を設定
            entity.setDistance(resultSet.getDouble("distance"));

            this.entityLot.add(entity);
        }
    }

    @Override
    public void selectAll(Connection connection) throws SQLException {
        // 未実装
    }
}
