package copel.sesproductpackage.core.unit;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

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

import copel.sesproductpackage.core.api.gpt.GptAnswer;
import copel.sesproductpackage.core.api.gpt.Transformer;
import copel.sesproductpackage.core.util.Properties;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.regions.Region;

/**
 * スキルシートの情報を持つクラス.
 *
 * @author 鈴木一矢
 *
 */
@Slf4j
public class SkillSheet {
    /**
     * 要約用プロンプト.
     */
    private final static String SKILLSHEET_SUMMARIZE_PROMPT = Properties.get("SKILLSHEET_SUMMARIZE_PROMPT");
    /**
     * DBへ保存するスキルシートのraw_contentの最大長.
     */
    private final static int SES_AI_T_SKILLSHEET_MAX_RAW_CONTENT_LENGTH = Properties.getInt("SES_AI_T_SKILLSHEET_MAX_RAW_CONTENT_LENGTH");
    /**
     * スキルシートの保存先S3バケット名.
     */
    private final static String S3_BUCKET_NAME = Properties.get("S3_BUCKET_NAME");

    /**
     * ファイルID.
     */
    private String fileId;
    /**
     * ファイル名.
     */
    private String fileName;
    /**
     * ファイル内容.
     */
    private String fileContent;
    /**
     * ファイル内容要約.
     */
    private String fileContentSummary;

    /**
     * デフォルトコンストラクタ.
     */
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
     * @throws IOException
     */
    public void setFileContentFromByte(final byte[] data) throws IOException {
        // ファイル名が存在する場合のみ処理する
        if (this.fileName != null && data != null) {
            InputStream inputStream = new ByteArrayInputStream(data);

            // Wordファイルを処理
            if (this.fileName.endsWith(".docx")) {
                StringBuilder text = new StringBuilder();
                XWPFDocument doc = new XWPFDocument(inputStream);
                for (XWPFParagraph paragraph : doc.getParagraphs()) {
                    text.append(paragraph.getText()).append("\n");
                }
                for (XWPFTable table : doc.getTables()) {
                    for (int rowIdx = 0; rowIdx < table.getRows().size(); rowIdx++) {
                        for (int cellIdx = 0; cellIdx < table.getRow(rowIdx).getTableCells().size(); cellIdx++) {
                            XWPFTableCell cell = table.getRow(rowIdx).getCell(cellIdx);
                            text.append(cell.getText()).append("\t");
                        }
                        text.append("\n");
                    }
                }
                doc.close();
                this.fileContent = text.toString();
            }
            // Wordファイルを処理
            else if (this.fileName.endsWith(".doc")) {
                StringBuilder text = new StringBuilder();
                 HWPFDocument doc = new HWPFDocument(inputStream);
                 WordExtractor extractor = new WordExtractor(doc);
                 for (String paragraphText : extractor.getParagraphText()) {
                	 text.append(paragraphText);
                 }
                 text.append(extractor.getText());
                 this.fileContent = text.toString();
                 extractor.close();
                 doc.close();
            }
            // PDFファイルを処理
            else if (this.fileName.endsWith(".pdf")) {
                PDDocument document = PDDocument.load(inputStream);
                PDFTextStripper stripper = new PDFTextStripper();
                this.fileContent = stripper.getText(document);
                document.close();
            }
            // Excelファイルを処理
            else if (this.fileName.endsWith(".xlsx") || this.fileName.endsWith(".xls")) {
                StringBuilder text = new StringBuilder();
                Workbook workbook = this.fileName.endsWith(".xlsx") ? new XSSFWorkbook(inputStream) : new HSSFWorkbook(inputStream);
                for (Row row : workbook.getSheetAt(0)) {
                    for (Cell cell : row) {
                        CustomCell customCell = new CustomCell(cell);
                        text.append(customCell.getValue(workbook.getCreationHelper().createFormulaEvaluator())).append(",");
                    }
                    text.append("\n");
                }
                workbook.close();
                this.fileContent = text.toString();
            }
            // その他のファイルの場合
            else {
            	log.info("Word/Excel/PDF以外のファイルのため、スキルシート内容の取得処理をせずに終了します。");
            }
        }
    }

    /**
     * このスキルシートの要約を生成しfileContentSummaryにセットします.
     *
     * @throws IOException 
     * @throws RuntimeException 
     */
    public void generateSummary(final Transformer transformer) throws IOException, RuntimeException {
        if (this.fileContent != null) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(SKILLSHEET_SUMMARIZE_PROMPT);
            stringBuilder.append(this.fileContent.replaceAll("[\\p{C}\"]", "")); // 制御文字とダブルクォーテーションを削除
            GptAnswer answer = transformer.generate(stringBuilder.toString());
            this.fileContentSummary = answer.getAnswer();
            // 1000文字制限を超えないように調整
            this.fileContentSummary = this.fileContentSummary.substring(0, Math.min(1000, this.fileContentSummary.length()));
        } else {
            throw new IOException("ファイルの中身が空のため、要約の作成を中止します。");
        }
    }

    /**
     * このスキルシートのダウンロードURLを返却します.
     *
     * @return ダウンロードURL
     */
    public String getFileUrl() {
        return "https://" + S3_BUCKET_NAME + ".s3." + Region.AP_NORTHEAST_1.toString() + ".amazonaws.com/" + this.getObjectKey();
    }

    /**
     * 紐づくS3ファイルのオブジェクトキーを返却する.
     *
     * @return オブジェクトキー.
     */
    public String getObjectKey() {
        return this.fileId + "_" + this.fileName;
    }

    // GETTER / SETTER
    public String getFileId() {
        return fileId;
    }
    public void setFileId(String fileId) {
        this.fileId = fileId;
    }
    public String getFileName() {
        return fileName;
    }
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
    public String getFileContent() {
        if (this.fileContent == null) {
            return null;
        }
        return this.fileContent.length() > SES_AI_T_SKILLSHEET_MAX_RAW_CONTENT_LENGTH ? this.fileContent.substring(0, SES_AI_T_SKILLSHEET_MAX_RAW_CONTENT_LENGTH - 1) : this.fileContent;
    }
    public void setFileContent(String fileContent) {
        this.fileContent = fileContent;
    }
    public String getFileContentSummary() {
        return fileContentSummary;
    }
    public void setFileContentSummary(String fileContentSummary) {
        this.fileContentSummary = fileContentSummary;
    }
}
