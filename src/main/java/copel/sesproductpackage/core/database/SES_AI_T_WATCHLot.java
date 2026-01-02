package copel.sesproductpackage.core.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import copel.sesproductpackage.core.database.SES_AI_T_WATCH.TargetType;
import copel.sesproductpackage.core.database.base.EntityLotBase;
import copel.sesproductpackage.core.unit.OriginalDateTime;

/**
 * 【Entityクラス】
 * ウォッチ管理テーブル(SES_AI_T_WATCH)のLotクラス.
 *
 * @author 鈴木一矢
 *
 */
public class SES_AI_T_WATCHLot extends EntityLotBase<SES_AI_T_WATCH> {
    /**
     * 全件SELECT文.
     */
    private final static String SELECT_ALL_SQL
        = "SELECT user_id, target_id, target_type, memo, register_date, register_user, ttl FROM SES_AI_T_WATCH";
    /**
     * SELECT文(ユーザーIDキー).
     */
    private final static String SELECT_BY_USER_ID_SQL
        = "SELECT user_id, target_id, target_type, memo, register_date, register_user, ttl FROM SES_AI_T_WATCH WHERE user_id = ?";

    public SES_AI_T_WATCHLot() {
        super();
    }

    /**
     * ユーザーIDが一致するレコードを全て取得し、このLotに格納します.
     *
     * @param connection DBコネクション
     * @param userId ユーザーID
     * @throws SQLException
     */
    public void selectByUserId(Connection connection, String userId) throws SQLException {
        if (connection == null || userId == null) {
            return;
        }
        PreparedStatement preparedStatement = connection.prepareStatement(SELECT_BY_USER_ID_SQL);
        preparedStatement.setString(1, userId);
        ResultSet resultSet = preparedStatement.executeQuery();
        this.entityLot = new ArrayList<SES_AI_T_WATCH>();
        while (resultSet.next()) {
            SES_AI_T_WATCH SES_AI_T_WATCH = new SES_AI_T_WATCH();
            SES_AI_T_WATCH.setUserId(resultSet.getString("user_id"));
            SES_AI_T_WATCH.setTargetId(resultSet.getString("target_id"));
            SES_AI_T_WATCH.setTargetType(TargetType.getEnumByName(resultSet.getString("target_type")));
            SES_AI_T_WATCH.setMemo(resultSet.getString("memo"));
            SES_AI_T_WATCH.setRegisterDate(new OriginalDateTime(resultSet.getString("register_date")));
            SES_AI_T_WATCH.setRegisterUser(resultSet.getString("register_user"));
            SES_AI_T_WATCH.setTtl(new OriginalDateTime(resultSet.getString("ttl")));
            this.entityLot.add(SES_AI_T_WATCH);
        }
    }

    @Override
    public void selectAll(Connection connection) throws SQLException {
        PreparedStatement preparedStatement = connection.prepareStatement(SELECT_ALL_SQL);
        ResultSet resultSet = preparedStatement.executeQuery();
        this.entityLot = new ArrayList<SES_AI_T_WATCH>();
        while (resultSet.next()) {
            SES_AI_T_WATCH SES_AI_T_WATCH = new SES_AI_T_WATCH();
            SES_AI_T_WATCH.setUserId(resultSet.getString("user_id"));
            SES_AI_T_WATCH.setTargetId(resultSet.getString("target_id"));
            SES_AI_T_WATCH.setTargetType(TargetType.getEnumByName(resultSet.getString("target_type")));
            SES_AI_T_WATCH.setMemo(resultSet.getString("memo"));
            SES_AI_T_WATCH.setRegisterDate(new OriginalDateTime(resultSet.getString("register_date")));
            SES_AI_T_WATCH.setRegisterUser(resultSet.getString("register_user"));
            SES_AI_T_WATCH.setTtl(new OriginalDateTime(resultSet.getString("ttl")));
            this.entityLot.add(SES_AI_T_WATCH);
        }
    }
}

