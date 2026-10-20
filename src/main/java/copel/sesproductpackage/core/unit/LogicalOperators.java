package copel.sesproductpackage.core.unit;

/**
 * 論理演算子.
 *
 * @author Copel Co., Ltd.
 */
public class LogicalOperators {
  /** 論理演算子. */
  private LogicalOperatorType logicOperator;

  /** 対象のカラム名. */
  private String columnName;

  /** 検索条件値. */
  private String value;

  /**
   * コンストラクタ.
   *
   * @param logicOperator 論理演算子
   * @param value 値
   */
  public LogicalOperators(final LogicalOperatorType logicOperator, final String value) {
    this.logicOperator = logicOperator;
    this.value = value;
  }

  /**
   * 論理演算子・カラム名・値を指定して検索条件を生成します。
   *
   * @param logicOperator 論理演算子
   * @param columnName カラム名
   * @param value 値
   */
  public LogicalOperators(
      final LogicalOperatorType logicOperator, final String columnName, final String value) {
    this.logicOperator = logicOperator;
    this.columnName = columnName;
    this.value = value;
  }

  /**
   * LIKE句を生成します.
   *
   * @return LIKE句
   */
  public String getLikeQuery() {
    if (this.logicOperator == null || this.columnName == null) {
      return null;
    } else {
      return " " + this.logicOperator.name() + " " + this.columnName + " LIKE ?";
    }
  }

  /**
   * 検索条件値を返却する.
   *
   * @return 検索条件値
   */
  public String getValue() {
    return this.value;
  }

  /**
   * カラム名を設定する.
   *
   * @param columnName カラム名
   */
  public void setColumnName(String columnName) {
    this.columnName = columnName;
  }

  /**
   * 論理演算子列挙型クラス.
   *
   * @author Copel Co., Ltd.
   */
  public enum LogicalOperatorType {
    AND,
    OR,
    NOT,
    NOR,
    XOR
  }
}
