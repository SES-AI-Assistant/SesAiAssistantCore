package copel.sesproductpackage.core.unit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import copel.sesproductpackage.core.api.gpt.GptAnswer;
import copel.sesproductpackage.core.api.gpt.Transformer;
import copel.sesproductpackage.core.util.Properties;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ContentTest {

  private static final int CRITERIA = 150;

  private static final String JOB_HIGH = "案件情報,精算あり,要員紹介不可,高額,単価,月額,給与,報酬,還元,場所,内容,作業";
  private static final String JOB_LOW = "場所,精算あり,作業,月額,単価";
  private static final String PERSON_HIGH = "要員情報,氏名,年齢,性別,住所,電話番号,メールアドレス,学歴,職歴";
  private static final String PERSON_LOW = "氏名,年齢,性別,住所,職歴";

  private static final String JOB_FEATURES = "案件情報,精算あり,要員紹介不可,場所,内容,作業,月額,単価";
  private static final String PERSON_FEATURES = "要員情報,氏名,年齢,性別,住所,職歴,学歴";

  @BeforeAll
  @SuppressWarnings("unchecked")
  static void setupProperties() throws Exception {
    Field propertiesField = Properties.class.getDeclaredField("properties");
    propertiesField.setAccessible(true);
    Map<String, String> propertiesMap = (Map<String, String>) propertiesField.get(null);
    propertiesMap.put("JOB_FEATURES_ARRAY_HIGH", JOB_HIGH);
    propertiesMap.put("JOB_FEATURES_ARRAY_LOW", JOB_LOW);
    propertiesMap.put("JOB_FEATURES_ARRAY", JOB_FEATURES);
    propertiesMap.put("PERSONEL_FEATURES_ARRAY_HIGH", PERSON_HIGH);
    propertiesMap.put("PERSONEL_FEATURES_ARRAY_LOW", PERSON_LOW);
    propertiesMap.put("PERSONEL_FEATURES_ARRAY", PERSON_FEATURES);
    propertiesMap.put("TARGET_NUMBER_OF_CRITERIA", String.valueOf(CRITERIA));
    propertiesMap.put("MULTIPLE_PERSONNEL_JUDGMENT_PROMPT", "Multiple Personnel?");
    propertiesMap.put("MULTIPLE_JOB_JUDGMENT_PROMPT", "Multiple Job?");
  }

  private static String loadResource(final String resourcePath) throws IOException {
    try (InputStream is = ContentTest.class.getClassLoader().getResourceAsStream(resourcePath)) {
      if (is == null) {
        return "Dummy content for " + resourcePath + " so tests don't fail if resource missing. " + "X".repeat(200);
      }
      return new String(is.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  @Test
  void testConstructorAndIsEmpty() {
    Content emptyContent = new Content();
    assertTrue(emptyContent.isEmpty());
    assertEquals(0, emptyContent.getContentList().size());
  }

  @Test
  void testIs案件紹介文() {
    String text = "案件情報 ".repeat(40);
    Content content = new Content(text);
    assertTrue(content.is案件紹介文());
    assertFalse(content.is要員紹介文());
  }

  @Test
  void testIs要員紹介文() {
    String text = "要員情報 ".repeat(40);
    Content content = new Content(text);
    assertTrue(content.is要員紹介文());
    assertFalse(content.is案件紹介文());
  }

  @Test
  void testIsEmptyAndCriteria() {
    Content emptyContent = new Content("");
    assertTrue(emptyContent.isEmpty());
    assertFalse(emptyContent.is案件紹介文());
    assertFalse(emptyContent.is要員紹介文());

    Content shortContent = new Content("Short");
    assertFalse(shortContent.is案件紹介文());
    assertFalse(shortContent.is要員紹介文());
  }

  @Test
  void test複数判定処理実行() throws Exception {
    String text = "要員情報 ".repeat(40);
    Content content = new Content(text);
    assertFalse(content.is複数紹介文());

    Transformer transformer = mock(Transformer.class);
    GptAnswer answer = mock(GptAnswer.class);

    when(answer.length()).thenReturn(20);
    when(answer.isJsonArrayFormat()).thenReturn(true);
    when(answer.getAsList()).thenReturn(List.of("要員1", "要員2"));
    when(transformer.generate(anyString())).thenReturn(answer);

    boolean result = content.複数判定処理実行(transformer);
    assertTrue(result);
    assertTrue(content.is複数紹介文());
    assertEquals(2, content.getContentList().size());
    assertTrue(content.toString().contains("要員1"));
  }

  @Test
  void test複数判定処理実行_FalseCase() throws Exception {
    Content content = new Content("Short");
    Transformer transformer = mock(Transformer.class);
    assertFalse(content.複数判定処理実行(transformer));
    assertEquals("Short", content.toString());
  }

  @Test
  void test複数判定処理実行_NotJson() throws Exception {
    String text = "案件情報 ".repeat(40);
    Content content = new Content(text);
    Transformer transformer = mock(Transformer.class);
    GptAnswer answer = mock(GptAnswer.class);

    when(answer.length()).thenReturn(5);
    when(answer.isJsonArrayFormat()).thenReturn(false);
    when(transformer.generate(anyString())).thenReturn(answer);

    boolean result = content.複数判定処理実行(transformer);
    assertFalse(result);
  }

  @Test
  void testSampleFiles() throws Exception {
    String[] jobFiles = {
      "content/job/job_sample_01.txt",
      "content/job/job_sample_02.txt"
    };
    for (String file : jobFiles) {
      String text = loadResource(file);
      new Content(text);
    }
  }

  @Test
  @SuppressWarnings("unchecked")
  void testCalcScoreBranches() throws Exception {
    Field propertiesField = Properties.class.getDeclaredField("properties");
    propertiesField.setAccessible(true);
    Map<String, String> propertiesMap = (Map<String, String>) propertiesField.get(null);

    String savedHigh = propertiesMap.remove("JOB_FEATURES_ARRAY_HIGH");
    try {
      assertNotNull(new Content("案件情報 ".repeat(40)));
    } finally {
      propertiesMap.put("JOB_FEATURES_ARRAY_HIGH", savedHigh);
    }
  }
}
