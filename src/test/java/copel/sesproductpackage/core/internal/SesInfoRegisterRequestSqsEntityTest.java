package copel.sesproductpackage.core.internal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import com.amazonaws.regions.Regions;

import copel.sesproductpackage.core.api.line.LineMessagingAPI;
import copel.sesproductpackage.core.unit.RequestType;
import copel.sesproductpackage.core.util.Properties;
import copel.sesproductpackage.core.util.SsmParameterKey;

@ExtendWith(MockitoExtension.class)
@DisplayName("SesInfoRegisterRequestSqsEntity テスト")
class SesInfoRegisterRequestSqsEntityTest {

  private SesInfoRegisterRequestSqsEntity entity;

  @BeforeEach
  void setUp() {
    entity = new SesInfoRegisterRequestSqsEntity();
  }

  @Test
  @DisplayName("デフォルトコンストラクタでインスタンス生成")
  void testDefaultConstructor() {
    SesInfoRegisterRequestSqsEntity instance = new SesInfoRegisterRequestSqsEntity();
    assertNotNull(instance);
    assertFalse(instance.isWatching());
  }

  @Test
  @DisplayName("SQS送信用コンストラクタでインスタンス生成")
  void testSqsConstructor() {
    try (MockedStatic<Properties> mockProperties = mockStatic(Properties.class)) {
      mockProperties
          .when(() -> Properties.get(SsmParameterKey.REGISTER_QUEUE_URL.getKey()))
          .thenReturn("http://sqs.example.com/queue");

      SesInfoRegisterRequestSqsEntity instance =
          new SesInfoRegisterRequestSqsEntity(Regions.AP_NORTHEAST_1);
      assertNotNull(instance);
    }
  }

  @Test
  @DisplayName("getMessageBody() が JSON を返す")
  void testGetMessageBody() {
    entity.setRequestType(RequestType.ScreenMessage);
    entity.setFromGroup("WEBAPP");
    entity.setFromId("user1");
    entity.setRawContent("Test content");
    entity.setTenantId("tenant1");

    String json = entity.getMessageBody();
    assertNotNull(json);
    assertTrue(json.contains("\"request_type\""));
    assertTrue(json.contains("\"from_group\""));
  }

  @Test
  @DisplayName("isValid() - LineMessage 正常系")
  void testIsValidLineMessageSuccess() {
    entity.setRequestType(RequestType.LineMessage);
    entity.setFromGroup("LINE");
    entity.setFromId("user1");
    entity.setFromName("User Name");
    entity.setRawContent("a".repeat(100));

    assertTrue(entity.isValid());
  }

  @Test
  @DisplayName("isValid() - LineMessage 異常系（fromGroupなし）")
  void testIsValidLineMessageNoFromGroup() {
    entity.setRequestType(RequestType.LineMessage);
    entity.setFromId("user1");
    entity.setFromName("User Name");
    entity.setRawContent("a".repeat(100));

    assertFalse(entity.isValid());
  }

  @Test
  @DisplayName("isValid() - LineMessage 異常系（fromIdなし）")
  void testIsValidLineMessageNoFromId() {
    entity.setRequestType(RequestType.LineMessage);
    entity.setFromGroup("LINE");
    entity.setFromName("User Name");
    entity.setRawContent("a".repeat(100));

    assertFalse(entity.isValid());
  }

  @Test
  @DisplayName("isValid() - LineMessage 異常系（fromNameなし）")
  void testIsValidLineMessageNoFromName() {
    entity.setRequestType(RequestType.LineMessage);
    entity.setFromGroup("LINE");
    entity.setFromId("user1");
    entity.setRawContent("a".repeat(100));

    assertFalse(entity.isValid());
  }

  @Test
  @DisplayName("isValid() - LineMessage 異常系（rawContentなし）")
  void testIsValidLineMessageNoRawContent() {
    entity.setRequestType(RequestType.LineMessage);
    entity.setFromGroup("LINE");
    entity.setFromId("user1");
    entity.setFromName("User Name");

    assertFalse(entity.isValid());
  }

  @Test
  @DisplayName("isValid() - LineMessage 異常系（rawContentが短い）")
  void testIsValidLineMessageShortContent() {
    entity.setRequestType(RequestType.LineMessage);
    entity.setFromGroup("LINE");
    entity.setFromId("user1");
    entity.setFromName("User Name");
    entity.setRawContent("short");

    assertFalse(entity.isValid());
  }

  @Test
  @DisplayName("isValid() - LineFile 正常系")
  void testIsValidLineFileSuccess() {
    entity.setRequestType(RequestType.LineFile);
    entity.setFromGroup("LINE");
    entity.setFromId("user1");
    entity.setFromName("User Name");
    entity.setFileName("test.pdf");

    assertTrue(entity.isValid());
  }

  @Test
  @DisplayName("isValid() - LineFile 異常系（fileNameなし）")
  void testIsValidLineFileNoFileName() {
    entity.setRequestType(RequestType.LineFile);
    entity.setFromGroup("LINE");
    entity.setFromId("user1");
    entity.setFromName("User Name");

    assertFalse(entity.isValid());
  }

  @Test
  @DisplayName("isValid() - EmailMessage 正常系")
  void testIsValidEmailMessageSuccess() {
    entity.setRequestType(RequestType.EmailMessage);
    entity.setFromGroup("EMAIL");
    entity.setFromId("user1");
    entity.setRawContent("a".repeat(100));

    assertTrue(entity.isValid());
  }

  @Test
  @DisplayName("isValid() - EmailMessage 異常系（fromIdなし）")
  void testIsValidEmailMessageNoFromId() {
    entity.setRequestType(RequestType.EmailMessage);
    entity.setFromGroup("EMAIL");
    entity.setRawContent("a".repeat(100));

    assertFalse(entity.isValid());
  }

  @Test
  @DisplayName("isValid() - EmailFile 正常系")
  void testIsValidEmailFileSuccess() {
    entity.setRequestType(RequestType.EmailFile);
    entity.setFromGroup("EMAIL");
    entity.setFromId("user1");
    entity.setFileName("test.pdf");

    assertTrue(entity.isValid());
  }

  @Test
  @DisplayName("isValid() - OtherMessage 正常系")
  void testIsValidOtherMessageSuccess() {
    entity.setRequestType(RequestType.OtherMessage);
    entity.setFromGroup("OTHER");
    entity.setFromId("user1");
    entity.setFromName("User Name");
    entity.setRawContent("a".repeat(100));

    assertTrue(entity.isValid());
  }

  @Test
  @DisplayName("isValid() - OtherFile 正常系")
  void testIsValidOtherFileSuccess() {
    entity.setRequestType(RequestType.OtherFile);
    entity.setFromGroup("OTHER");
    entity.setFromId("user1");
    entity.setFromName("User Name");
    entity.setFileName("test.pdf");

    assertTrue(entity.isValid());
  }

  @Test
  @DisplayName("isValid() - ScreenMessage 正常系")
  void testIsValidScreenMessageSuccess() {
    entity.setRequestType(RequestType.ScreenMessage);
    entity.setFromGroup("WEBAPP");
    entity.setFromId("user1");
    entity.setRawContent("Test content");

    assertTrue(entity.isValid());
  }

  @Test
  @DisplayName("isValid() - ScreenMessage 異常系（rawContentなし）")
  void testIsValidScreenMessageNoRawContent() {
    entity.setRequestType(RequestType.ScreenMessage);
    entity.setFromGroup("WEBAPP");
    entity.setFromId("user1");

    assertFalse(entity.isValid());
  }

  @Test
  @DisplayName("isValid() - ScreenFile 正常系")
  void testIsValidScreenFileSuccess() {
    entity.setRequestType(RequestType.ScreenFile);
    entity.setFromGroup("WEBAPP");
    entity.setFromId("user1");
    entity.setFileName("test.pdf");

    assertTrue(entity.isValid());
  }

  @Test
  @DisplayName("isValid() - infoTypeInvalidが true の場合は false を返す")
  void testIsValidInfoTypeInvalidFlag() {
    entity.setRequestType(RequestType.ScreenMessage);
    entity.setFromGroup("WEBAPP");
    entity.setFromId("user1");
    entity.setRawContent("Test content");
    entity.setInfoTypeInvalid(true);

    assertFalse(entity.isValid());
  }

  @Test
  @DisplayName("isValid() - 不正な RequestType")
  void testIsValidUnknownRequestType() {
    assertFalse(entity.isValid());
  }

  @Test
  @DisplayName("isDirectedJobRegistration() - 画面指定の案件登録")
  void testIsDirectedJobRegistrationSuccess() {
    entity.setRequestType(RequestType.ScreenMessage);
    entity.setInfoType("JOB");
    entity.setRawContent("Test job content");

    assertTrue(entity.isDirectedJobRegistration());
  }

  @Test
  @DisplayName("isDirectedJobRegistration() - infoType が null")
  void testIsDirectedJobRegistrationNullInfoType() {
    entity.setRequestType(RequestType.ScreenMessage);
    entity.setRawContent("Test job content");

    assertFalse(entity.isDirectedJobRegistration());
  }

  @Test
  @DisplayName("isDirectedJobRegistration() - infoType が PERSON")
  void testIsDirectedJobRegistrationPersonType() {
    entity.setRequestType(RequestType.ScreenMessage);
    entity.setInfoType("PERSON");
    entity.setRawContent("Test job content");

    assertFalse(entity.isDirectedJobRegistration());
  }

  @Test
  @DisplayName("isDirectedJobRegistration() - requestType が ScreenMessage ではない")
  void testIsDirectedJobRegistrationWrongRequestType() {
    entity.setRequestType(RequestType.LineMessage);
    entity.setInfoType("JOB");
    entity.setRawContent("Test job content");

    assertFalse(entity.isDirectedJobRegistration());
  }

  @Test
  @DisplayName("isDirectedJobRegistration() - rawContent が空")
  void testIsDirectedJobRegistrationEmptyContent() {
    entity.setRequestType(RequestType.ScreenMessage);
    entity.setInfoType("JOB");
    entity.setRawContent("");

    assertFalse(entity.isDirectedJobRegistration());
  }

  @Test
  @DisplayName("isDirectedCandidateRegistration() - 画面指定の要員登録（ScreenMessage）")
  void testIsDirectedCandidateRegistrationScreenMessage() {
    entity.setRequestType(RequestType.ScreenMessage);
    entity.setInfoType("PERSON");
    entity.setRawContent("Test person content");

    assertTrue(entity.isDirectedCandidateRegistration());
  }

  @Test
  @DisplayName("isDirectedCandidateRegistration() - 画面指定の要員登録（ScreenFile）")
  void testIsDirectedCandidateRegistrationScreenFile() {
    entity.setRequestType(RequestType.ScreenFile);
    entity.setInfoType("PERSON");

    assertTrue(entity.isDirectedCandidateRegistration());
  }

  @Test
  @DisplayName("isDirectedCandidateRegistration() - infoType が null")
  void testIsDirectedCandidateRegistrationNullInfoType() {
    entity.setRequestType(RequestType.ScreenMessage);
    entity.setRawContent("Test person content");

    assertFalse(entity.isDirectedCandidateRegistration());
  }

  @Test
  @DisplayName("isDirectedCandidateRegistration() - infoType が JOB")
  void testIsDirectedCandidateRegistrationJobType() {
    entity.setRequestType(RequestType.ScreenMessage);
    entity.setInfoType("JOB");
    entity.setRawContent("Test person content");

    assertFalse(entity.isDirectedCandidateRegistration());
  }

  @Test
  @DisplayName("isDirectedCandidateRegistration() - ScreenMessage なのに rawContent が空")
  void testIsDirectedCandidateRegistrationEmptyContent() {
    entity.setRequestType(RequestType.ScreenMessage);
    entity.setInfoType("PERSON");
    entity.setRawContent("");

    assertFalse(entity.isDirectedCandidateRegistration());
  }

  @Test
  @DisplayName("isDirectedCandidateRegistration() - 不正な requestType")
  void testIsDirectedCandidateRegistrationInvalidRequestType() {
    entity.setRequestType(RequestType.LineMessage);
    entity.setInfoType("PERSON");
    entity.setRawContent("Test person content");

    assertFalse(entity.isDirectedCandidateRegistration());
  }

  @Test
  @DisplayName("isスキルシート() - LineFile")
  void testIsSkillsheetLineFile() {
    entity.setRequestType(RequestType.LineFile);
    entity.setFileName("test.pdf");

    assertTrue(entity.isスキルシート());
  }

  @Test
  @DisplayName("isスキルシート() - EmailFile")
  void testIsSkillsheetEmailFile() {
    entity.setRequestType(RequestType.EmailFile);
    entity.setFileName("test.pdf");

    assertTrue(entity.isスキルシート());
  }

  @Test
  @DisplayName("isスキルシート() - ScreenFile")
  void testIsSkillsheetScreenFile() {
    entity.setRequestType(RequestType.ScreenFile);
    entity.setFileName("test.pdf");

    assertTrue(entity.isスキルシート());
  }

  @Test
  @DisplayName("isスキルシート() - OtherFile")
  void testIsSkillsheetOtherFile() {
    entity.setRequestType(RequestType.OtherFile);
    entity.setFileName("test.pdf");

    assertTrue(entity.isスキルシート());
  }

  @Test
  @DisplayName("isスキルシート() - File type なのに fileName が null")
  void testIsSkillsheetFileNoFileName() {
    entity.setRequestType(RequestType.LineFile);

    assertFalse(entity.isスキルシート());
  }

  @Test
  @DisplayName("isスキルシート() - File type なのに fileName が空")
  void testIsSkillsheetFileEmptyFileName() {
    entity.setRequestType(RequestType.LineFile);
    entity.setFileName("");

    assertFalse(entity.isスキルシート());
  }

  @Test
  @DisplayName("isスキルシート() - Message type")
  void testIsSkillsheetMessageType() {
    entity.setRequestType(RequestType.LineMessage);
    entity.setFileName("test.pdf");

    assertFalse(entity.isスキルシート());
  }

  @Test
  @DisplayName("downloadFileData() - LINE ファイル")
  void testDownloadFileDataLineFile() throws IOException, InterruptedException {
    entity.setRequestType(RequestType.LineFile);
    entity.setFileId("file1");
    byte[] testData = "test content".getBytes();

    LineMessagingAPI mockClient = mock(LineMessagingAPI.class);
    when(mockClient.getFile("file1")).thenReturn(testData);

    try (var mockLineAPI = mockStatic(LineMessagingAPI.class)) {
      mockLineAPI.when(() -> new LineMessagingAPI("token")).thenReturn(mockClient);

      entity.downloadFileData("token");
      assertArrayEquals(testData, entity.getFileData());
    }
  }

  @Test
  @DisplayName("downloadFileData() - EMAIL ファイル（何もしない）")
  void testDownloadFileDataEmailFile() throws IOException, InterruptedException {
    entity.setRequestType(RequestType.EmailFile);
    entity.setFileData(null);

    entity.downloadFileData("token");
    assertNull(entity.getFileData());
  }

  @Test
  @DisplayName("downloadFileData() - SCREEN ファイル（何もしない）")
  void testDownloadFileDataScreenFile() throws IOException, InterruptedException {
    entity.setRequestType(RequestType.ScreenFile);
    entity.setFileData(null);

    entity.downloadFileData("token");
    assertNull(entity.getFileData());
  }

  @Test
  @DisplayName("downloadFileData() - OTHER ファイル（何もしない）")
  void testDownloadFileDataOtherFile() throws IOException, InterruptedException {
    entity.setRequestType(RequestType.OtherFile);
    entity.setFileData(null);

    entity.downloadFileData("token");
    assertNull(entity.getFileData());
  }

  @Test
  @DisplayName("downloadFileData() - Message type（何もしない）")
  void testDownloadFileDataMessageType() throws IOException, InterruptedException {
    entity.setRequestType(RequestType.LineMessage);
    entity.setFileData(null);

    entity.downloadFileData("token");
    assertNull(entity.getFileData());
  }

  @Test
  @DisplayName("Lombok @Data の getter/setter が機能する")
  void testLombokGettersSetters() {
    entity.setRequestType(RequestType.ScreenMessage);
    entity.setFromGroup("WEBAPP");
    entity.setFromId("user1");
    entity.setFromName("User Name");
    entity.setRawContent("content");
    entity.setFileId("file1");
    entity.setFileName("test.pdf");
    byte[] fileData = "data".getBytes();
    entity.setFileData(fileData);
    entity.setWatching(true);
    entity.setInfoType("JOB");
    entity.setInfoTypeInvalid(true);
    entity.setTenantId("tenant1");

    assertEquals(RequestType.ScreenMessage, entity.getRequestType());
    assertEquals("WEBAPP", entity.getFromGroup());
    assertEquals("user1", entity.getFromId());
    assertEquals("User Name", entity.getFromName());
    assertEquals("content", entity.getRawContent());
    assertEquals("file1", entity.getFileId());
    assertEquals("test.pdf", entity.getFileName());
    assertArrayEquals(fileData, entity.getFileData());
    assertTrue(entity.isWatching());
    assertEquals("JOB", entity.getInfoType());
    assertTrue(entity.isInfoTypeInvalid());
    assertEquals("tenant1", entity.getTenantId());
  }
}
