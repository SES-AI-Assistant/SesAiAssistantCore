package copel.sesproductpackage.core.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import org.apache.poi.ss.formula.eval.NotImplementedException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** {@link CustomCell} の単体テスト。 */
class CustomCellTest {

  /** テストで使う Excel のワークブック。メモリ上だけで作る。 */
  private XSSFWorkbook workbook;

  /** テストで使うシート。 */
  private Sheet sheet;

  @BeforeEach
  void ワークブックとシートを作る() {
    workbook = new XSSFWorkbook();
    sheet = workbook.createSheet();
  }

  @AfterEach
  void ワークブックを閉じる() throws IOException {
    workbook.close();
  }

  /**
   * テスト用の空のセルを作る。
   *
   * @return 1行目1列目のセル
   */
  private Cell 空のセル() {
    return sheet.createRow(0).createCell(0);
  }

  @Nested
  class getValue {

    @Nested
    class 正常系 {

      /**
       * 数値のセルを数値の文字列として返すことを確かめる。 日付と誤認しやすい小数を、そのままの文字列にして値を失わないようにする。
       *
       * @given セルの値が 1.5 の数値
       * @when getValue を数式の評価器つきで実行する
       * @then "1.5" を返す
       */
      @Test
      void 小数の数値の場合_数値の文字列を返す() {
        Cell cell = 空のセル();
        cell.setCellValue(1.5);

        assertThat(new CustomCell(cell).getValue(評価器())).isEqualTo("1.5");
      }

      /**
       * 日付を示す数値のセルを yyyy/MM/dd の文字列として返すことを確かめる。 Excel の日付のシリアル値は数値のままだと読めないので、日付の文字列に変換する。
       *
       * @given セルの値が 45000 の数値
       * @when getValue を数式の評価器つきで実行する
       * @then "2023/03/15" を返す
       */
      @Test
      void 日付を示す数値の場合_yyyy_MM_ddの文字列を返す() {
        Cell cell = 空のセル();
        cell.setCellValue(45000);

        assertThat(new CustomCell(cell).getValue(評価器())).isEqualTo("2023/03/15");
      }

      /**
       * 文字列のセルをそのままの文字列で返すことを確かめる。 文字列のセルは内容を変えずに取り出さないと、入力された値が変わってしまう。
       *
       * @given セルの値が "abc" の文字列
       * @when getValue を数式の評価器つきで実行する
       * @then "abc" を返す
       */
      @Test
      void 文字列の場合_そのままの文字列を返す() {
        Cell cell = 空のセル();
        cell.setCellValue("abc");

        assertThat(new CustomCell(cell).getValue(評価器())).isEqualTo("abc");
      }

      /**
       * 真偽値のセルを true の文字列として返すことを確かめる。 真偽値のセルは文字列の表記を揃えて出さないと、後続の出力形式が崩れる。
       *
       * @given セルの値が true の真偽値
       * @when getValue を数式の評価器つきで実行する
       * @then "true" を返す
       */
      @Test
      void 真偽値の場合_trueの文字列を返す() {
        Cell cell = 空のセル();
        cell.setCellValue(true);

        assertThat(new CustomCell(cell).getValue(評価器())).isEqualTo("true");
      }

      /**
       * 数式のセルを評価した結果の文字列を返すことを確かめる。 数式のセルは式そのものが保存されているので、評価結果を取り出さないと内容が読めない。
       *
       * @given セルの数式が "1+2"
       * @when getValue を数式の評価器つきで実行する
       * @then 計算結果の "3.0" を返す
       */
      @Test
      void 数式の場合_計算結果の文字列を返す() {
        Cell cell = 空のセル();
        cell.setCellFormula("1+2");

        assertThat(new CustomCell(cell).getValue(評価器())).isEqualTo("3.0");
      }
    }

    @Nested
    class 異常系 {

      /**
       * 評価器が未対応の数式を投げた場合は、セルの文字列表現を返すことを確かめる。 評価できない数式で変換が例外になると、ファイル全体の解析が止まるので、セルの表記で代替する。
       *
       * @given セルの数式が "1+2"、評価器が NotImplementedException を投げる
       * @when getValue を評価器つきで実行する
       * @then セルの文字列表現を返す
       */
      @Test
      void 数式が未対応の場合_セルの文字列表現を返す() throws Exception {
        Cell cell = 空のセル();
        cell.setCellFormula("1+2");
        FormulaEvaluator evaluator = mock(FormulaEvaluator.class);
        when(evaluator.evaluate(any(Cell.class))).thenThrow(new NotImplementedException("未対応の関数"));

        assertThat(new CustomCell(cell).getValue(evaluator)).isEqualTo(cell.toString());
      }
    }

    @Nested
    class 境界値 {

      /**
       * 空のセルは空文字を返すことを確かめる。 空のセルを空文字として返さないと、連結時に null が混入して出力が崩れる。
       *
       * @given セルが空
       * @when getValue を数式の評価器つきで実行する
       * @then 空文字を返す
       */
      @Test
      void 空のセルの場合_空文字を返す() {
        assertThat(new CustomCell(空のセル()).getValue(評価器())).isEmpty();
      }
    }
  }

  @Nested
  class isExcelDate {

    @Nested
    class 正常系 {

      /**
       * 日付の範囲内の整数を日付として判定することを確かめる。 日付のシリアル値の範囲内の整数は、Excel の仕様どおり日付として扱う必要がある。
       *
       * @given セルの値が 45000
       * @when isExcelDate を実行する
       * @then true を返す
       */
      @Test
      void 日付の範囲内の整数の場合_trueを返す() {
        Cell cell = 空のセル();
        cell.setCellValue(45000);

        assertThat(new CustomCell(cell).isExcelDate()).isTrue();
      }
    }

    @Nested
    class 異常系 {

      /**
       * 負の値は日付ではないと判定することを確かめる。 負の値は Excel の日付シリアルとして成立しないので、日付扱いにしない。
       *
       * @given セルの値が -1
       * @when isExcelDate を実行する
       * @then false を返す
       */
      @Test
      void 負の数の場合_falseを返す() {
        Cell cell = 空のセル();
        cell.setCellValue(-1);

        assertThat(new CustomCell(cell).isExcelDate()).isFalse();
      }
    }

    @Nested
    class 境界値 {

      /**
       * 範囲の下限の 1 を日付として判定することを確かめる。 日付の下限の値を取り違えると、範囲の端の判定が誤るので、境界を固定する。
       *
       * @given セルの値が 1
       * @when isExcelDate を実行する
       * @then true を返す
       */
      @Test
      void 下限の1の場合_trueを返す() {
        Cell cell = 空のセル();
        cell.setCellValue(1);

        assertThat(new CustomCell(cell).isExcelDate()).isTrue();
      }

      /**
       * 範囲の上限の 2958465 を日付として判定することを確かめる。 日付の上限の値を取り違えると、範囲の端の判定が誤るので、境界を固定する。
       *
       * @given セルの値が 2958465
       * @when isExcelDate を実行する
       * @then true を返す
       */
      @Test
      void 上限の2958465の場合_trueを返す() {
        Cell cell = 空のセル();
        cell.setCellValue(2958465);

        assertThat(new CustomCell(cell).isExcelDate()).isTrue();
      }

      /**
       * 範囲の上限を超える 2958466 は日付ではないと判定することを確かめる。 上限を超えた値は Excel の日付として表示できないので、日付ではないと判定する。
       *
       * @given セルの値が 2958466
       * @when isExcelDate を実行する
       * @then false を返す
       */
      @Test
      void 上限を超える2958466の場合_falseを返す() {
        Cell cell = 空のセル();
        cell.setCellValue(2958466);

        assertThat(new CustomCell(cell).isExcelDate()).isFalse();
      }

      /**
       * 範囲の下限より小さい 0 は日付ではないと判定することを確かめる。 0 は Excel の日付の範囲外なので、数値のまま扱う必要がある。
       *
       * @given セルの値が 0
       * @when isExcelDate を実行する
       * @then false を返す
       */
      @Test
      void 下限未満の0の場合_falseを返す() {
        Cell cell = 空のセル();
        cell.setCellValue(0);

        assertThat(new CustomCell(cell).isExcelDate()).isFalse();
      }

      /**
       * 小数の値は日付ではないと判定することを確かめる。 小数の値は時刻を含むので、日付のシリアル値として扱わず数値のまま出す。
       *
       * @given セルの値が 1.5
       * @when isExcelDate を実行する
       * @then false を返す
       */
      @Test
      void 小数の場合_falseを返す() {
        Cell cell = 空のセル();
        cell.setCellValue(1.5);

        assertThat(new CustomCell(cell).isExcelDate()).isFalse();
      }
    }
  }

  /**
   * テスト用の数式の評価器を作る。
   *
   * @return ワークブックの数式の評価器
   */
  private FormulaEvaluator 評価器() {
    return workbook.getCreationHelper().createFormulaEvaluator();
  }
}
