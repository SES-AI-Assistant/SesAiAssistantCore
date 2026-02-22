package copel.sesproductpackage.core.unit;

/**
 * 論理演算子.
 *
 * @author 鈴木一矢
 */
public class LogicalOperators {
  /** 論理演算子. */
  private 論理演算子 logicOperator;

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
  public LogicalOperators(final 論理演算子 logicOperator, final String value) {
    this.logicOperator = logicOperator;
    this.value = value;
  }

  public LogicalOperators(final 論理演算子 logicOperator, final String columnName, final String value) {
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
   * @author 鈴木一矢
   */
  public enum 論理演算子 {
    AND,
    OR,
    NOT,
    NOR,
    XOR
  }
}
