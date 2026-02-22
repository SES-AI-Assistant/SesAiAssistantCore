package copel.sesproductpackage.core.database.base;

import copel.sesproductpackage.core.unit.OriginalDateTime;
import java.sql.Connection;
import java.sql.SQLException;
import lombok.Data;

/**
 * エンティティの基底クラス.
 *
 * @author 鈴木一矢
 */
@Data
public abstract class EntityBase implements Comparable<EntityBase> {
  /** 登録日時 / register_date */
  @Column(required = true, physicalName = "register_date", logicalName = "登録日時")
  protected OriginalDateTime registerDate;

  /** 登録ユーザー / register_user */
  @Column(required = true, physicalName = "register_user", logicalName = "登録ユーザー")
  protected String registerUser;

  /**
   * INSERT処理を実行します.
   *
   * @param connection DBコネクション
   * @return 登録成功可否
   * @throws SQLException
   */
  public abstract int insert(Connection connection) throws SQLException;

  /**
   * PKをキーにSELECT処理を実行します.
   *
   * @param connection DBコネクション
   * @throws SQLException
   */
  public abstract void selectByPk(final Connection connection) throws SQLException;

  /**
   * PKをキーにUPDATE処理を実行します.
   *
   * @param connection DBコネクション
   * @throws SQLException
   */
  public abstract boolean updateByPk(final Connection connection) throws SQLException;

  /**
   * PKをキーにDELETE処理を実行します.
   *
   * @param connection DBコネクション
   * @throws SQLException
   */
  public abstract boolean deleteByPk(final Connection connection) throws SQLException;

  @Override
  public int compareTo(EntityBase o) {
    if (this.registerDate == null) {
      return o == null || o.getRegisterDate() == null ? 0 : -1;
    } else if (o == null || o.getRegisterDate() == null) {
      return 1;
    } else {
      return this.registerDate.compareTo(o.getRegisterDate());
    }
  }
}
