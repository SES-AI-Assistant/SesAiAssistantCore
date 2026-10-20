package copel.sesproductpackage.core.unit;

import copel.sesproductpackage.core.util.Properties;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import software.amazon.awssdk.regions.Region;

/**
 * スキルシートの情報を持つクラス.
 *
 * @author Copel Co., Ltd.
 */
@Slf4j
@Data
public class SkillSheet {
  /** ファイルID. */
  private String fileId;

  /** ファイル名. */
  private String fileName;

  /** ファイル内容. */
  @Getter(AccessLevel.NONE)
  private String fileContent;

  /** ファイル内容要約. */
  private String fileContentSummary;

  /** デフォルトコンストラクタ. */
  public SkillSheet() {}

  /**
   * コンストラクタ.
   *
   * @param fileId ファイルID
   * @param fileName ファイル名
   * @param fileContent ファイル内容
   */
  public SkillSheet(final String fileId, final String fileName, final String fileContent) {
    this.fileId = fileId;
    this.fileName = fileName;
    this.fileContent = fileContent;
  }

  /**
   * ファイルのバイナリデータからファイルに記載されているテキストデータを取得しこのクラスのfileContentにセットします.
   *
   * @param data バイナリデータ
   * @throws IOException 入出力（ファイル・ストリーム）の操作に失敗した場合
   */
  public void setFileContentFromByte(final byte[] data) throws IOException {
    if (this.fileName != null && data != null) {
      try {
        // Base64にエンコード
        String base64 = java.util.Base64.getEncoder().encodeToString(data);

        // markitdown-lambdaを呼び出すリクエストを構築
        copel.sesproductpackage.core.api.markitdown.MarkItDown.MarkitdownLambdaRequestEntity mdReq =
            copel.sesproductpackage.core.api.markitdown.MarkItDown.MarkitdownLambdaRequestEntity
                .builder()
                .fileBase64(base64)
                .filename(this.fileName)
                .build();

        log.info("markitdown-lambda を呼び出してスキルシートをパースします。ファイル名: {}", this.fileName);
        copel.sesproductpackage.core.api.markitdown.MarkItDown.MarkitdownLambdaResponseEntity
            mdRes = copel.sesproductpackage.core.api.markitdown.MarkItDown.invoke(mdReq);

        if (mdRes.isSuccess() && mdRes.getMarkdown() != null && !mdRes.getMarkdown().isBlank()) {
          this.fileContent = mdRes.getMarkdown();
          log.info("markitdown-lambda によるパースに成功しました。文字数: {}", this.fileContent.length());
        } else {
          String errMsg = mdRes.getError() != null ? mdRes.getError().getMessage() : "不明なエラー";
          log.error("markitdown-lambda によるパースが失敗しました: {}", errMsg);
          throw new IOException("markitdown-lambda によるパース失敗: " + errMsg);
        }
      } catch (Exception e) {
        log.error("markitdown-lambda の呼び出し中にエラーが発生しました。従来のローカルパース処理へフォールバックします。", e);
        // フォールバックとして従来の Apache POI / PDFBox によるパースを走らせる
        fallbackLocalParse(data);
      }
    }
  }

  /** 従来のローカルパース（Apache POI / PDFBox）によるフォールバック処理. */
  private void fallbackLocalParse(final byte[] data) throws IOException {
    InputStream inputStream = new ByteArrayInputStream(data);

    // Wordファイルを処理
    if (this.fileName.endsWith(".docx")) {
      StringBuilder text = new StringBuilder();
      try (XWPFDocument doc = new XWPFDocument(inputStream)) {
        for (XWPFParagraph paragraph : doc.getParagraphs()) {
          text.append(paragraph.getText()).append("\n");
        }
        for (XWPFTable table : doc.getTables()) {
          for (int rowIdx = 0; rowIdx < table.getRows().size(); rowIdx++) {
            for (int cellIdx = 0;
                cellIdx < table.getRow(rowIdx).getTableCells().size();
                cellIdx++) {
              XWPFTableCell cell = table.getRow(rowIdx).getCell(cellIdx);
              text.append(cell.getText()).append("\t");
            }
            text.append("\n");
          }
        }
      }
      this.fileContent = text.toString();
    } else if (this.fileName.endsWith(".doc")) { // Wordファイルを処理
      StringBuilder text = new StringBuilder();
      try (HWPFDocument doc = new HWPFDocument(inputStream);
          WordExtractor extractor = new WordExtractor(doc)) {
        for (String paragraphText : extractor.getParagraphText()) {
          text.append(paragraphText);
        }
        text.append(extractor.getText());
        this.fileContent = text.toString();
      }
    } else if (this.fileName.endsWith(".pdf")) { // PDFファイルを処理
      try (PDDocument document = PDDocument.load(inputStream)) {
        PDFTextStripper stripper = new PDFTextStripper();
        this.fileContent = stripper.getText(document);
      }
    } else if (this.fileName.endsWith(".xlsx") || this.fileName.endsWith(".xls")) { // Excelファイルを処理
      StringBuilder text = new StringBuilder();
      try (Workbook workbook =
          this.fileName.endsWith(".xlsx")
              ? new XSSFWorkbook(inputStream)
              : new HSSFWorkbook(inputStream)) {
        for (Row row : workbook.getSheetAt(0)) {
          for (Cell cell : row) {
            CustomCell customCell = new CustomCell(cell);
            text.append(customCell.getValue(workbook.getCreationHelper().createFormulaEvaluator()))
                .append(",");
          }
          text.append("\n");
        }
        this.fileContent = text.toString();
      }
    } else { // その他のファイルの場合
      log.info("Word/Excel/PDF以外のファイルのため、スキルシート内容の取得処理をせずに終了します。");
    }
  }

  /**
   * このスキルシートのダウンロードURLを返却します.
   *
   * <p>保存キーが決まらない（ファイル ID とファイル名が両方とも未設定）場合は null を返す。キーの無い "…/null" の URL を作らないためである。
   *
   * <p>バケット名（S3_BUCKET_NAME）は、クラス初期化時ではなく呼び出し時に Properties
   * から読む。本番では起動時に読み込み済みのため値は変わらず、テストで設定値を差し替えられるようにするため。
   *
   * @return ダウンロードURL。保存キーが null の場合は null
   */
  public String getFileUrl() {
    String objectKey = this.getObjectKey();
    if (objectKey == null) {
      return null;
    }
    return "https://"
        + Properties.get("S3_BUCKET_NAME")
        + ".s3."
        + Region.AP_NORTHEAST_1.toString()
        + ".amazonaws.com/"
        + objectKey;
  }

  /**
   * 紐づくS3ファイルのオブジェクトキーを返却する.
   *
   * <p>ファイル ID とファイル名の両方がある場合は、ファイル ID がファイル名で終わるときはファイル ID を、そうでないときは「ID_ファイル名」を返す。片方だけがある場合は
   * その値を返す。両方とも未設定の場合は保存キーを決められないため null を返す。
   *
   * @return オブジェクトキー。両方とも未設定の場合は null
   */
  public String getObjectKey() {
    if (this.fileId == null) {
      return this.fileName;
    }
    if (this.fileName == null) {
      return this.fileId;
    }
    if (this.fileId.endsWith("_" + this.fileName)) {
      return this.fileId;
    }
    return this.fileId + "_" + this.fileName;
  }

  /**
   * 抽出したファイルの本文を返します。
   *
   * <p>上限（SES_AI_T_SKILLSHEET_MAX_RAW_CONTENT_LENGTH）を超える本文は、先頭から上限の文字数ちょうどに切り詰める。上限が 0
   *
   * <p>上限も、クラス初期化時ではなく呼び出し時に Properties から読む。本番の値は変わらず、テストで差し替えられるようにするため。 以下の場合は全文を返す。
   *
   * @return 本文（未抽出の場合は null）
   */
  public String getFileContent() {
    if (this.fileContent == null) {
      return null;
    }
    int maxLength = Properties.getInt("SES_AI_T_SKILLSHEET_MAX_RAW_CONTENT_LENGTH");
    if (maxLength <= 0) {
      return this.fileContent;
    }
    return this.fileContent.length() > maxLength
        ? this.fileContent.substring(0, maxLength)
        : this.fileContent;
  }
}
