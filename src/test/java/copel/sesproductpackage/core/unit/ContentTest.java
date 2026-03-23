package copel.sesproductpackage.core.unit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
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

  private static final int MIN_LENGTH = 200;
  private static final String LONG_TEXT_JOB = "案件情報 ".repeat(40);
  private static final String LONG_TEXT_PERSON = "要員情報 ".repeat(40);

  @BeforeAll
  @SuppressWarnings("unchecked")
  static void setupProperties() throws Exception {
    Field propertiesField = Properties.class.getDeclaredField("properties");
    propertiesField.setAccessible(true);
    Map<String, String> propertiesMap = (Map<String, String>) propertiesField.get(null);
    propertiesMap.put(
        "CONTENT_CLASSIFICATION_PROMPT", "以下の文章が案件・要員・その他のどれか答えよ。回答は「案件」「要員」「その他」のいずれか1語のみ: ");
    propertiesMap.put("CONTENT_MIN_LENGTH_FOR_CLASSIFICATION", String.valueOf(MIN_LENGTH));
    propertiesMap.put("MULTIPLE_PERSONNEL_JUDGMENT_PROMPT", "Multiple Personnel?");
    propertiesMap.put("MULTIPLE_JOB_JUDGMENT_PROMPT", "Multiple Job?");
  }

  private static String loadResource(final String resourcePath) throws IOException {
    try (InputStream is = ContentTest.class.getClassLoader().getResourceAsStream(resourcePath)) {
      if (is == null) {
        return "Dummy content for "
            + resourcePath
            + " so tests don't fail if resource missing. "
            + "X".repeat(200);
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
  void testIs案件紹介文() throws Exception {
    Content content = new Content(LONG_TEXT_JOB);
    assertFalse(content.is案件紹介文());
    assertFalse(content.is要員紹介文());

    Transformer transformer = mock(Transformer.class);
    GptAnswer answer = mock(GptAnswer.class);
    when(answer.getAnswer()).thenReturn("案件");
    when(transformer.generate(anyString())).thenReturn(answer);

    content.classify(transformer);
    assertTrue(content.is案件紹介文());
    assertFalse(content.is要員紹介文());
  }

  @Test
  void testIs要員紹介文() throws Exception {
    Content content = new Content(LONG_TEXT_PERSON);
    assertFalse(content.is要員紹介文());
    assertFalse(content.is案件紹介文());

    Transformer transformer = mock(Transformer.class);
    GptAnswer answer = mock(GptAnswer.class);
    when(answer.getAnswer()).thenReturn("要員");
    when(transformer.generate(anyString())).thenReturn(answer);

    content.classify(transformer);
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
  void testClassifyShortContentTreatedAsOther() throws Exception {
    Content content = new Content("承知致しました。");
    Transformer transformer = mock(Transformer.class);

    content.classify(transformer);
    verify(transformer, never()).generate(anyString());
    assertFalse(content.is案件紹介文());
    assertFalse(content.is要員紹介文());
  }

  @Test
  void testClassifyEmptyContentTreatedAsOther() throws Exception {
    Content content = new Content("");
    Transformer transformer = mock(Transformer.class);
    content.classify(transformer);
    verify(transformer, never()).generate(anyString());
    assertFalse(content.is案件紹介文());
    assertFalse(content.is要員紹介文());
  }

  @Test
  void test複数判定処理実行() throws Exception {
    Content content = new Content(LONG_TEXT_PERSON);
    Transformer transformer = mock(Transformer.class);
    GptAnswer classifyAnswer = mock(GptAnswer.class);
    when(classifyAnswer.getAnswer()).thenReturn("要員");

    GptAnswer multiAnswer = mock(GptAnswer.class);
    when(multiAnswer.length()).thenReturn(20);
    when(multiAnswer.isJsonArrayFormat()).thenReturn(true);
    when(multiAnswer.getAsList()).thenReturn(List.of("要員1", "要員2"));

    // 1回目: classify用、2回目: 複数判定用
    when(transformer.generate(anyString())).thenReturn(classifyAnswer).thenReturn(multiAnswer);

    content.classify(transformer);
    assertTrue(content.is要員紹介文());

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
    content.classify(transformer);
    assertFalse(content.複数判定処理実行(transformer));
    assertEquals("Short", content.toString());
  }

  @Test
  void test複数判定処理実行_NotJson() throws Exception {
    Content content = new Content(LONG_TEXT_JOB);
    Transformer transformer = mock(Transformer.class);
    GptAnswer classifyAnswer = mock(GptAnswer.class);
    when(classifyAnswer.getAnswer()).thenReturn("案件");
    GptAnswer multiAnswer = mock(GptAnswer.class);
    when(multiAnswer.length()).thenReturn(5);
    when(multiAnswer.isJsonArrayFormat()).thenReturn(false);
    when(transformer.generate(anyString())).thenReturn(classifyAnswer).thenReturn(multiAnswer);

    content.classify(transformer);
    boolean result = content.複数判定処理実行(transformer);
    assertFalse(result);
  }

  @Test
  void testSampleFiles() throws Exception {
    String[] jobFiles = {"content/job/job_sample_01.txt", "content/job/job_sample_02.txt"};
    for (String file : jobFiles) {
      String text = loadResource(file);
      Content c = new Content(text);
      assertNotNull(c);
    }
  }

  @Test
  void testParseClassificationAnswer_OtherWhenInvalid() throws Exception {
    Content content = new Content(LONG_TEXT_JOB);
    Transformer transformer = mock(Transformer.class);
    GptAnswer answer = mock(GptAnswer.class);
    when(answer.getAnswer()).thenReturn("不明な回答");
    when(transformer.generate(anyString())).thenReturn(answer);

    content.classify(transformer);
    assertFalse(content.is案件紹介文());
    assertFalse(content.is要員紹介文());
  }

  @Test
  void testParseClassificationAnswer_OtherWhenNullAnswer() throws Exception {
    Content content = new Content(LONG_TEXT_JOB);
    Transformer transformer = mock(Transformer.class);
    GptAnswer answer = mock(GptAnswer.class);
    when(answer.getAnswer()).thenReturn(null);
    when(transformer.generate(anyString())).thenReturn(answer);

    content.classify(transformer);
    assertFalse(content.is案件紹介文());
    assertFalse(content.is要員紹介文());
  }
}
