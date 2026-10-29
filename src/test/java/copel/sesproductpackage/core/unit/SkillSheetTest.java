package copel.sesproductpackage.core.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import copel.sesproductpackage.core.util.Properties;
import copel.sesproductpackage.core.util.SsmParameterKey;
import copel.sesproductpackage.tools.testsupport.clock.FixedClock;
import copel.sesproductpackage.tools.testsupport.config.TestProperty;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.LambdaClientBuilder;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import software.amazon.awssdk.services.lambda.model.InvokeResponse;

/** {@link SkillSheet} の単体テスト。 */
@FixedClock("2026-01-15T12:00:00+09:00")
class SkillSheetTest {

  /** markitdown-lambda の呼び出しを差し替えるための Lambda クライアントのモック。 */
  private LambdaClient lambdaClient;

  /** Lambda クライアントの構築を差し替えるハンドル。テスト後に解除する。 */
  private MockedStatic<LambdaClient> lambdaStatic;

  /** SkillSheet が出力するログを集めるアペンダー。ログの重大度を確かめるテストだけが使う。 */
  private final ListAppender<ILoggingEvent> logAppender = new ListAppender<>();

  @BeforeEach
  void Lambdaの呼び出しを差し替える() {
    logAppender.start();
    ((Logger) LoggerFactory.getLogger(SkillSheet.class)).addAppender(logAppender);
    lambdaClient = mock(LambdaClient.class);
    LambdaClientBuilder builder = mock(LambdaClientBuilder.class, RETURNS_SELF);
    when(builder.build()).thenReturn(lambdaClient);
    lambdaStatic = mockStatic(LambdaClient.class);
    lambdaStatic.when(LambdaClient::builder).thenReturn(builder);
    // markitdown-lambda の関数名は呼び出し時に設定から読むため、テストでは名前を設定しておく
    Properties.putForTest(SsmParameterKey.MARKITDOWN_NAME.getKey(), "markitdown-fn");
  }

  @AfterEach
  void Lambdaの差し替えを解除する() {
    ((Logger) LoggerFactory.getLogger(SkillSheet.class)).detachAppender(logAppender);
    logAppender.stop();
    lambdaStatic.close();
    Properties.removeForTest(SsmParameterKey.MARKITDOWN_NAME.getKey());
  }

  /**
   * markitdown-lambda が指定の応答を返すように設定する。
   *
   * @param json 応答の JSON 文字列
   */
  private void markitdownが応答する(final String json) {
    when(lambdaClient.invoke(any(InvokeRequest.class)))
        .thenReturn(InvokeResponse.builder().payload(SdkBytes.fromUtf8String(json)).build());
  }

  /** markitdown-lambda の呼び出しが失敗するように設定し、ローカルの解析へ進ませる。 */
  private void markitdownが失敗する() {
    lambdaStatic.when(LambdaClient::builder).thenThrow(new IllegalStateException("起動失敗"));
  }

  /**
   * Excel の xlsx 形式のバイト列を作る。1 行目に文字列 "a" と数値 1.5 を入れる。
   *
   * @return xlsx のバイト列
   * @throws IOException 作成に失敗した場合
   */
  private static byte[] xlsxを作る() throws IOException {
    try (XSSFWorkbook workbook = new XSSFWorkbook();
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      Row row = workbook.createSheet().createRow(0);
      row.createCell(0).setCellValue("a");
      row.createCell(1).setCellValue(1.5);
      workbook.write(out);
      return out.toByteArray();
    }
  }

  /**
   * Excel の xls 形式のバイト列を作る。1 行目に文字列 "a" と数値 1.5 を入れる。
   *
   * @return xls のバイト列
   * @throws IOException 作成に失敗した場合
   */
  private static byte[] xlsを作る() throws IOException {
    try (HSSFWorkbook workbook = new HSSFWorkbook();
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      Sheet sheet = workbook.createSheet();
      Row row = sheet.createRow(0);
      Cell first = row.createCell(0);
      first.setCellValue("a");
      row.createCell(1).setCellValue(1.5);
      workbook.write(out);
      return out.toByteArray();
    }
  }

  /**
   * Word の docx 形式のバイト列を作る。段落に "本文"、表の 1 セルに "セル" を入れる。
   *
   * @return docx のバイト列
   * @throws IOException 作成に失敗した場合
   */
  private static byte[] docxを作る() throws IOException {
    try (XWPFDocument document = new XWPFDocument();
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      document.createParagraph().createRun().setText("本文");
      XWPFTable table = document.createTable(1, 1);
      table.getRow(0).getCell(0).setText("セル");
      document.write(out);
      return out.toByteArray();
    }
  }

  /**
   * Word の doc 形式のバイト列を読み込む。本文は "本文" の 1 行である。
   *
   * <p>HWPF は空の文書を作れないため、macOS の textutil で作った固定のファイルを使う。
   *
   * @return doc のバイト列
   * @throws IOException 読み込みに失敗した場合
   */
  private static byte[] docを読み込む() throws IOException {
    try (InputStream in = SkillSheetTest.class.getResourceAsStream("/fixtures/skill-sample.doc")) {
      return in.readAllBytes();
    }
  }

  /**
   * PDF のバイト列を作る。本文に "Hello" を入れる。
   *
   * @return pdf のバイト列
   * @throws IOException 作成に失敗した場合
   */
  private static byte[] pdfを作る() throws IOException {
    try (PDDocument document = new PDDocument();
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      PDPage page = new PDPage();
      document.addPage(page);
      try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
        stream.beginText();
        stream.setFont(PDType1Font.HELVETICA, 12);
        stream.newLineAtOffset(50, 700);
        stream.showText("Hello");
        stream.endText();
      }
      document.save(out);
      return out.toByteArray();
    }
  }

  @Nested
  class コンストラクタ {

    @Nested
    class 正常系 {

      /**
       * 引数なしで生成した場合、各項目が未設定であることを確かめる。 解析前の状態を空として扱わないと解析結果と混同するので、未設定の状態にする。
       *
       * @given 引数を指定しない
       * @when 生成する
       * @then ファイルの項目が null のインスタンスを返す
       */
      @Test
      void 引数なしで生成した場合_各項目が未設定のインスタンスを返す() {
        SkillSheet skillSheet = new SkillSheet();

        assertThat(skillSheet.getFileId()).isNull();
        assertThat(skillSheet.getFileName()).isNull();
        assertThat(skillSheet.getFileContentSummary()).isNull();
      }
    }
  }

  @Nested
  class コンストラクタ_String_String_String {

    @Nested
    class 正常系 {

      /**
       * ファイルの ID と名前と内容を指定した場合、その値を保持することを確かめる。 読み込んだスキルシートの情報を後続の処理へ渡さないと情報が失われるので、値を保持する。
       *
       * @given ファイル ID が "F001"、ファイル名が "skill.xlsx"、内容が "本文"
       * @when 生成する
       * @then 指定した ID と名前を保持し、内容の取得で "本文" を返す
       */
      @Test
      void ファイルの情報を指定した場合_指定した値を保持するインスタンスを返す() {
        SkillSheet skillSheet = new SkillSheet("F001", "skill.xlsx", "本文");

        assertThat(skillSheet.getFileId()).isEqualTo("F001");
        assertThat(skillSheet.getFileName()).isEqualTo("skill.xlsx");
        assertThat(skillSheet.getFileContent()).isEqualTo("本文");
      }
    }
  }

  @Nested
  class setFileContentFromByte {

    @Nested
    class 正常系 {

      /**
       * markitdown-lambda が本文を返す場合は、その本文をファイル内容に設定することを確かめる。 markitdown
       * の変換結果を使わないと精度の高い本文が無駄になるので、変換結果を優先する。
       *
       * @given ファイル名が "skill.docx"、markitdown-lambda が本文 "# 本文" を返す
       * @when setFileContentFromByte をバイト列で実行する
       * @then ファイル内容が "# 本文" になる
       * @throws IOException 読み込みに失敗した場合
       */
      @Test
      void markitdownが本文を返す場合_本文の内容を返す() throws IOException {
        markitdownが応答する("{\"success\":true,\"markdown\":\"# 本文\"}");
        SkillSheet skillSheet = new SkillSheet("F001", "skill.docx", null);

        skillSheet.setFileContentFromByte(new byte[] {1, 2, 3});

        assertThat(skillSheet.getFileContent()).isEqualTo("# 本文");
      }
    }

    @Nested
    class 異常系 {

      /**
       * markitdown-lambda が本文を返さない場合は、ローカルの解析の本文を返すことを確かめる。 変換が失敗しても Excel から本文を取り出せることを守る。
       *
       * @given markitdown-lambda が本文を返さない応答、ファイル名が "skill.xlsx"、Excel の内容がある
       * @when setFileContentFromByte をバイト列で実行する
       * @then ローカルの解析の本文 "a,1.5,\n" を返し、ERROR を出力しない
       * @throws IOException 読み込みに失敗した場合
       */
      @ValueSource(
          strings = {
            "{\"success\":false,\"error\":{\"type\":\"X\",\"message\":\"失敗\"}}",
            "{\"success\":false}",
            "{\"success\":true}",
            "{\"success\":true,\"markdown\":\"  \"}"
          })
      @ParameterizedTest
      void markitdownが本文を返さない場合_ローカル解析の本文を返す(final String json) throws IOException {
        markitdownが応答する(json);
        SkillSheet skillSheet = new SkillSheet("F001", "skill.xlsx", null);

        skillSheet.setFileContentFromByte(xlsxを作る());

        assertThat(skillSheet.getFileContent()).isEqualTo("a,1.5,\n");
        assertThat(logAppender.list)
            .extracting(ILoggingEvent::getLevel)
            .contains(Level.WARN)
            .doesNotContain(Level.ERROR);
      }

      /**
       * markitdown-lambda の呼び出しに失敗した場合は、docx のローカル解析の本文を返すことを確かめる。 Word
       * の段落と表が連結されないと表の内容が本文から抜けるので、段落と表を両方取り出す。
       *
       * @given markitdown-lambda の呼び出しが失敗する、ファイル名が "skill.docx"
       * @when setFileContentFromByte をバイト列で実行する
       * @then 段落の "本文" と表の "セル" を連結した文字列を返し、ERROR を出力しない
       * @throws IOException 読み込みに失敗した場合
       */
      @Test
      void docxの場合_段落と表の文字を連結した本文を返す() throws IOException {
        markitdownが失敗する();
        SkillSheet skillSheet = new SkillSheet("F001", "skill.docx", null);

        skillSheet.setFileContentFromByte(docxを作る());

        assertThat(skillSheet.getFileContent()).isEqualTo("本文\nセル\t\n");
        assertThat(logAppender.list)
            .extracting(ILoggingEvent::getLevel)
            .contains(Level.WARN)
            .doesNotContain(Level.ERROR);
      }

      /**
       * markitdown-lambda の呼び出しに失敗した場合は、doc のローカル解析の本文を返すことを確かめる。 古い Word
       * の本文を取り出せないと旧形式のスキルシートが読めないので、doc の経路を固定する。
       *
       * @given markitdown-lambda の呼び出しが失敗する、ファイル名が "skill.doc"
       * @when setFileContentFromByte をバイト列で実行する
       * @then 本文の "本文" を含む文字列を返す
       * @throws IOException 読み込みに失敗した場合
       */
      @Test
      void docの場合_本文を含む文字列を返す() throws IOException {
        markitdownが失敗する();
        SkillSheet skillSheet = new SkillSheet("F001", "skill.doc", null);

        skillSheet.setFileContentFromByte(docを読み込む());

        assertThat(skillSheet.getFileContent()).contains("本文");
      }

      /**
       * markitdown-lambda の呼び出しに失敗した場合は、pdf のローカル解析の本文を返すことを確かめる。 PDF の本文を取り出せないと PDF
       * のスキルシートが読めないので、pdf の経路を固定する。
       *
       * @given markitdown-lambda の呼び出しが失敗する、ファイル名が "skill.pdf"
       * @when setFileContentFromByte をバイト列で実行する
       * @then 本文の "Hello" を含む文字列を返す
       * @throws IOException 読み込みに失敗した場合
       */
      @Test
      void pdfの場合_本文を含む文字列を返す() throws IOException {
        markitdownが失敗する();
        SkillSheet skillSheet = new SkillSheet("F001", "skill.pdf", null);

        skillSheet.setFileContentFromByte(pdfを作る());

        assertThat(skillSheet.getFileContent()).contains("Hello");
      }

      /**
       * markitdown-lambda の呼び出しに失敗した場合は、xls のローカル解析の本文を返すことを確かめる。 旧形式の Excel
       * のセルを取り出せないと旧形式のスキルシートが読めないので、xls の経路を固定する。
       *
       * @given markitdown-lambda の呼び出しが失敗する、ファイル名が "skill.xls"
       * @when setFileContentFromByte をバイト列で実行する
       * @then セルの値をカンマと改行で連結した "a,1.5,\n" を返す
       * @throws IOException 読み込みに失敗した場合
       */
      @Test
      void xlsの場合_セルの値を連結した本文を返す() throws IOException {
        markitdownが失敗する();
        SkillSheet skillSheet = new SkillSheet("F001", "skill.xls", null);

        skillSheet.setFileContentFromByte(xlsを作る());

        assertThat(skillSheet.getFileContent()).isEqualTo("a,1.5,\n");
      }

      /**
       * 対象外の拡張子の場合は、ローカル解析でもファイル内容を設定しないことを確かめる。 対象外の形式を解析すると誤った本文が保存されるので、何もしない。
       *
       * @given markitdown-lambda の呼び出しが失敗する、ファイル名が "memo.txt"
       * @when setFileContentFromByte をバイト列で実行する
       * @then 何もしない
       * @throws IOException 読み込みに失敗した場合
       */
      @Test
      void 対象外の拡張子の場合_何もしない() throws IOException {
        markitdownが失敗する();
        SkillSheet skillSheet = new SkillSheet("F001", "memo.txt", null);

        skillSheet.setFileContentFromByte(new byte[] {1});

        assertThat(skillSheet.getFileContent()).isNull();
      }
    }

    @Nested
    class 境界値 {

      /**
       * ファイル名が未設定の場合は、何もしないことを確かめる。 ファイル名が無いと形式を判定できないので、解析せずに終わる。
       *
       * @given ファイル名が null、データが空でない
       * @when setFileContentFromByte をバイト列で実行する
       * @then 何もしない
       * @throws IOException 読み込みに失敗した場合
       */
      @Test
      void ファイル名が未設定の場合_何もしない() throws IOException {
        SkillSheet skillSheet = new SkillSheet("F001", null, null);

        skillSheet.setFileContentFromByte(new byte[] {1});

        verify(lambdaClient, org.mockito.Mockito.never()).invoke(any(InvokeRequest.class));
        assertThat(skillSheet.getFileContent()).isNull();
      }

      /**
       * データが null の場合は、何もしないことを確かめる。 データが無いと解析できないので、何もしない。
       *
       * @given ファイル名が "skill.docx"、データが null
       * @when setFileContentFromByte を null で実行する
       * @then 何もしない
       * @throws IOException 読み込みに失敗した場合
       */
      @Test
      void データがnullの場合_何もしない() throws IOException {
        SkillSheet skillSheet = new SkillSheet("F001", "skill.docx", null);

        skillSheet.setFileContentFromByte(null);

        verify(lambdaClient, org.mockito.Mockito.never()).invoke(any(InvokeRequest.class));
        assertThat(skillSheet.getFileContent()).isNull();
      }

      /**
       * 本文が空白で対象外の拡張子の場合は、何もしないことを確かめる。 解析できない形式の本文を設定すると誤った内容が保存されるので、何もしない。
       *
       * @given markitdown-lambda が空白だけの本文を返す、ファイル名が "memo.txt"
       * @when setFileContentFromByte をバイト列で実行する
       * @then 何もしない
       * @throws IOException 読み込みに失敗した場合
       */
      @Test
      void 本文が空白で対象外の拡張子の場合_何もしない() throws IOException {
        markitdownが応答する("{\"success\":true,\"markdown\":\"  \"}");
        SkillSheet skillSheet = new SkillSheet("F001", "memo.txt", null);

        skillSheet.setFileContentFromByte(new byte[] {1});

        assertThat(skillSheet.getFileContent()).isNull();
      }
    }
  }

  @Nested
  class getFileUrl {

    @Nested
    class 正常系 {

      /**
       * ファイルの公開 URL を、バケット名と東京リージョンと保存キーから組み立てることを確かめる。 URL の形式が変わると保存先の資料が開けなくなるので、形式を固定する。
       *
       * @given 設定値 S3_BUCKET_NAME が "nectar-bucket"、ファイル ID が "F001"、ファイル名が "skill.xlsx"
       * @when getFileUrl を実行する
       * @then 東京リージョンの S3 の URL を返す
       */
      @TestProperty(key = "S3_BUCKET_NAME", value = "nectar-bucket")
      @Test
      void ファイルIDとファイル名がある場合_S3の公開URLを返す() {
        SkillSheet skillSheet = new SkillSheet("F001", "skill.xlsx", null);

        assertThat(skillSheet.getFileUrl())
            .isEqualTo("https://nectar-bucket.s3.ap-northeast-1.amazonaws.com/F001_skill.xlsx");
      }
    }

    @Nested
    class 境界値 {

      /**
       * 保存キーが決められない場合は、ダウンロードURLも null を返すことを確かめる。 保存キーが無い状態で URL を作ると無効なリンクになるので、null を返す。
       *
       * @given ファイル ID とファイル名が null
       * @when getFileUrl を実行する
       * @then null を返す
       */
      @TestProperty(key = "S3_BUCKET_NAME", value = "nectar-bucket")
      @Test
      void 保存キーが無い場合_nullを返す() {
        assertThat(new SkillSheet(null, null, null).getFileUrl()).isNull();
      }
    }
  }

  @Nested
  class getObjectKey {

    @Nested
    class 正常系 {

      /**
       * ファイル ID がファイル名で終わらない場合は、ID とファイル名を連結したキーを返すことを確かめる。 キーの形式が変わると既存の保存先と一致しなくなるので、形式を固定する。
       *
       * @given ファイル ID が "F001"、ファイル名が "skill.xlsx"
       * @when getObjectKey を実行する
       * @then "F001_skill.xlsx" を返す
       */
      @Test
      void ファイルIDがファイル名で終わらない場合_IDとファイル名を連結したキーを返す() {
        assertThat(new SkillSheet("F001", "skill.xlsx", null).getObjectKey())
            .isEqualTo("F001_skill.xlsx");
      }

      /**
       * ファイル ID がファイル名で終わる場合は、ファイル ID をそのままキーとして返すことを確かめる。 同じファイル名が二重に付くと保存先のキーが見つからなくなるので、ID を返す。
       *
       * @given ファイル ID が "F001_skill.xlsx"、ファイル名が "skill.xlsx"
       * @when getObjectKey を実行する
       * @then "F001_skill.xlsx" を返す
       */
      @Test
      void ファイルIDがファイル名で終わる場合_ファイルIDを返す() {
        assertThat(new SkillSheet("F001_skill.xlsx", "skill.xlsx", null).getObjectKey())
            .isEqualTo("F001_skill.xlsx");
      }
    }

    @Nested
    class 境界値 {

      /**
       * ファイル ID が未設定でファイル名だけがある場合は、ファイル名をキーとして返すことを確かめる。
       * 片方の情報しかない場合も保存先を特定できないとスキルシートを取り出せないので、ファイル名を使う。
       *
       * @given ファイル ID が null、ファイル名が "skill.xlsx"
       * @when getObjectKey を実行する
       * @then "skill.xlsx" を返す
       */
      @Test
      void ファイルIDが未設定でファイル名がある場合_ファイル名を返す() {
        assertThat(new SkillSheet(null, "skill.xlsx", null).getObjectKey()).isEqualTo("skill.xlsx");
      }

      /**
       * ファイル名が未設定でファイル ID だけがある場合は、ファイル ID をキーとして返すことを確かめる。
       * 片方の情報しかない場合も保存先を特定できないとスキルシートを取り出せないので、ID を使う。
       *
       * @given ファイル ID が "F001"、ファイル名が null
       * @when getObjectKey を実行する
       * @then "F001" を返す
       */
      @Test
      void ファイル名が未設定でファイルIDがある場合_ファイルIDを返す() {
        assertThat(new SkillSheet("F001", null, null).getObjectKey()).isEqualTo("F001");
      }

      /**
       * ファイル ID とファイル名が両方未設定の場合は null を返すことを確かめる。 保存先を決められない状態で null を返さないと誤ったキーで検索してしまうので、null
       * を返す。
       *
       * @given ファイル ID とファイル名が null
       * @when getObjectKey を実行する
       * @then null を返す
       */
      @Test
      void ファイルIDとファイル名が両方未設定の場合_nullを返す() {
        assertThat(new SkillSheet(null, null, null).getObjectKey()).isNull();
      }
    }
  }

  @Nested
  class getFileContent {

    @Nested
    class 正常系 {

      /**
       * 本文が上限を超える場合は、上限の文字数ちょうどの先頭部分を返すことを確かめる。 上限を超えた本文を保存すると DB の列に入らないので、上限の文字数で切る。
       *
       * @given 設定値の上限が 5、ファイル内容が "abcdefgh"
       * @when getFileContent を実行する
       * @then 先頭 5 文字の "abcde" を返す
       */
      @TestProperty(key = "SES_AI_T_SKILLSHEET_MAX_RAW_CONTENT_LENGTH", value = "5")
      @Test
      void 本文が上限を超える場合_上限の文字数の先頭部分を返す() {
        assertThat(new SkillSheet("F001", "skill.xlsx", "abcdefgh").getFileContent())
            .isEqualTo("abcde");
      }
    }

    @Nested
    class 境界値 {

      /**
       * ファイル内容が未設定の場合は null を返すことを確かめる。 未抽出の本文を空文字として扱うと抽出済みと誤認するので、null を返す。
       *
       * @given ファイル内容が null
       * @when getFileContent を実行する
       * @then null を返す
       */
      @Test
      void ファイル内容が未設定の場合_nullを返す() {
        assertThat(new SkillSheet("F001", "skill.xlsx", null).getFileContent()).isNull();
      }

      /**
       * 上限が 0 の場合は、本文の全文を返すことを確かめる。 上限が未設定の環境で本文を切ると内容が欠けるので、全文を返す。
       *
       * @given 設定値の上限が 0、ファイル内容が "abcdefgh"
       * @when getFileContent を実行する
       * @then 全文の "abcdefgh" を返す
       */
      @TestProperty(key = "SES_AI_T_SKILLSHEET_MAX_RAW_CONTENT_LENGTH", value = "0")
      @Test
      void 上限が0の場合_全文を返す() {
        assertThat(new SkillSheet("F001", "skill.xlsx", "abcdefgh").getFileContent())
            .isEqualTo("abcdefgh");
      }

      /**
       * 本文が上限と同じ長さの場合は、全文を返すことを確かめる。 上限ちょうどの本文を切ると末尾の 1 文字が欠けるので、全文を返す。
       *
       * @given 設定値の上限が 5、ファイル内容が "abcde"
       * @when getFileContent を実行する
       * @then 全文の "abcde" を返す
       */
      @TestProperty(key = "SES_AI_T_SKILLSHEET_MAX_RAW_CONTENT_LENGTH", value = "5")
      @Test
      void 本文が上限と同じ長さの場合_全文を返す() {
        assertThat(new SkillSheet("F001", "skill.xlsx", "abcde").getFileContent())
            .isEqualTo("abcde");
      }
    }
  }
}
