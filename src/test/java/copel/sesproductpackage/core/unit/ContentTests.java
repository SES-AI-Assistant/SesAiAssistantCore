package copel.sesproductpackage.core.unit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import copel.sesproductpackage.core.api.gpt.GptAnswer;
import copel.sesproductpackage.core.api.gpt.Transformer;
import copel.sesproductpackage.core.util.Properties;

class ContentTests {

    @BeforeAll
    @SuppressWarnings("unchecked")
    static void setupProperties() throws Exception {
        Field propertiesField = Properties.class.getDeclaredField("properties");
        propertiesField.setAccessible(true);
        Map<String, String> propertiesMap = (Map<String, String>) propertiesField.get(null);
        propertiesMap.put("JOB_FEATURES_ARRAY", "Java,Python");
        propertiesMap.put("PERSONEL_FEATURES_ARRAY", "エンジニア,募集");
        propertiesMap.put("TARGET_NUMBER_OF_CRITERIA", "10");
        propertiesMap.put("MULTIPLE_PERSONNEL_JUDGMENT_PROMPT", "Multiple Personnel?");
        propertiesMap.put("MULTIPLE_JOB_JUDGMENT_PROMPT", "Multiple Job?");
    }

    @Test
    void testConstructorAndIsEmpty() {
        Content emptyContent = new Content();
        assertTrue(emptyContent.isEmpty());
        assertEquals(0, emptyContent.getContentList().size());
    }

    @Test
    void testIs案件紹介文() {
        // 10文字以上で、案件特徴ワード(Java, Python)が要員特徴ワード(エンジニア, 募集)より多い
        Content content = new Content("Java Java Java な案件です。10文字以上。");
        assertTrue(content.is案件紹介文());
        assertFalse(content.is要員紹介文());
    }

    @Test
    void testIs要員紹介文() {
        // 10文字以上で、要員特徴ワード(エンジニア, 募集)が案件特徴ワード(Java, Python)より多い
        Content content = new Content("エンジニア 募集 募集 です。10文字以上。");
        assertTrue(content.is要員紹介文());
        assertFalse(content.is案件紹介文());
    }

    @Test
    void testIs複数紹介文And複数判定処理実行() throws Exception {
        Content content = new Content("エンジニア 募集 募集 です。10文字以上。");
        assertFalse(content.is複数紹介文());

        Transformer transformer = mock(Transformer.class);
        GptAnswer answer = mock(GptAnswer.class);
        
        when(answer.length()).thenReturn(20);
        when(answer.isJsonArrayFormat()).thenReturn(true);
        when(answer.getAsList()).thenReturn(List.of("エンジニア1", "エンジニア2"));
        when(transformer.generate(anyString())).thenReturn(answer);

        boolean result = content.複数判定処理実行(transformer);
        assertTrue(result);
        assertTrue(content.is複数紹介文());
        assertEquals(2, content.getContentList().size());
        assertEquals("エンジニア1", content.getContentList().get(0));
    }

    @Test
    void testToString() {
        Content content = new Content("単一コンテンツ");
        assertEquals("単一コンテンツ", content.toString());

        // 複数リストがある場合
        // 内部の contentList に直接入れる手段がないので、複数判定処理を通す
    }
    
    @Test
    void test複数判定処理実行_FalseCase() throws Exception {
        Content content = new Content("短い");
        Transformer transformer = mock(Transformer.class);
        assertFalse(content.複数判定処理実行(transformer));
    }
}
