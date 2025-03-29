package copel.sesproductpackage.core.unit;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.poi.ss.formula.eval.NotImplementedException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.FormulaEvaluator;

/**
 * org.apache.poi.ss.usermodel.Cellのカスタムクラス.
 *
 * @author 鈴木一矢
 *
 */
public class CustomCell {
    /**
     * セル.
     */
    private Cell cell;

    /**
     * コンストラクタ.
     *
     * @param cell Cell
     */
    public CustomCell (final Cell cell) {
        this.cell = cell;
    }

    /**
     * このセルの型や数式を考慮し、評価した値を文字列で返却する.
     *
     * @param formulaEvaluator FormulaEvaluator
     * @return 値
     */
    public String getValue(FormulaEvaluator formulaEvaluator) {
        String value = "";
        switch (this.cell.getCellType()) {
            case NUMERIC:
                if (this.isExcelDate()) {
                    Date date = this.cell.getDateCellValue();
                    value = new SimpleDateFormat("yyyy/MM/dd").format(date);
                } else {
                    value = String.valueOf(this.cell.getNumericCellValue());
                }
                break;
            case STRING:
                value = this.cell.getStringCellValue();
                break;
            case BOOLEAN:
                value = String.valueOf(this.cell.getBooleanCellValue());
                break;
            case FORMULA:
                // 数式の場合、計算結果を取得
                try {
                    value = formulaEvaluator.evaluate(this.cell).formatAsString();
                } catch(NotImplementedException e) {
                    value = this.cell.toString();
                }
                break;
            default:
                break;
        }
        return value;
    }

    /**
     * このセルの値が日付を示す数値であるかどうかを判定する.
     *
     * @return 日付を示す数値であればtrue、そうでなければfalse
     */
    public boolean isExcelDate() {
        return this.cell.getNumericCellValue() >= 1.0 && this.cell.getNumericCellValue() <= 2958465.0 && this.cell.getNumericCellValue() == Math.floor(this.cell.getNumericCellValue());
    }
}
