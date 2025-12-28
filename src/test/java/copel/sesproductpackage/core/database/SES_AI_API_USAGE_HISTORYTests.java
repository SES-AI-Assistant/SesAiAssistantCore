package copel.sesproductpackage.core.database;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import copel.sesproductpackage.core.database.SES_AI_API_USAGE_HISTORY.ApiType;
import copel.sesproductpackage.core.database.SES_AI_API_USAGE_HISTORY.Provider;

class SES_AI_API_USAGE_HISTORYTests {

    @Test
    void saveTest() {
        SES_AI_API_USAGE_HISTORY SES_AI_API_USAGE_HISTORY = new SES_AI_API_USAGE_HISTORY();
        SES_AI_API_USAGE_HISTORY.setProvider(Provider.OpenAI);
        SES_AI_API_USAGE_HISTORY.setModel("gpt-4o-mini");
        SES_AI_API_USAGE_HISTORY.setUsageMonth("202501");
        SES_AI_API_USAGE_HISTORY.setUserId("test_user_id");
        SES_AI_API_USAGE_HISTORY.setApiType(ApiType.Generate);
        SES_AI_API_USAGE_HISTORY.setInputCount(new BigDecimal(10));
        SES_AI_API_USAGE_HISTORY.setOutputCount(new BigDecimal(1));
        SES_AI_API_USAGE_HISTORY.save();

        SES_AI_API_USAGE_HISTORY = new SES_AI_API_USAGE_HISTORY();
        SES_AI_API_USAGE_HISTORY.setProvider(Provider.OpenAI);
        SES_AI_API_USAGE_HISTORY.setModel("gpt-4o-mini");
        SES_AI_API_USAGE_HISTORY.setUsageMonth("202502");
        SES_AI_API_USAGE_HISTORY.setUserId("test_user_id");
        SES_AI_API_USAGE_HISTORY.setApiType(ApiType.Generate);
        SES_AI_API_USAGE_HISTORY.setInputCount(new BigDecimal(20));
        SES_AI_API_USAGE_HISTORY.setOutputCount(new BigDecimal(10));
        SES_AI_API_USAGE_HISTORY.save();

        SES_AI_API_USAGE_HISTORY = new SES_AI_API_USAGE_HISTORY();
        SES_AI_API_USAGE_HISTORY.setProvider(Provider.OpenAI);
        SES_AI_API_USAGE_HISTORY.setModel("gpt-4o-mini");
        SES_AI_API_USAGE_HISTORY.setUsageMonth("202501");
        SES_AI_API_USAGE_HISTORY.setUserId("test_user_id");
        SES_AI_API_USAGE_HISTORY.setApiType(ApiType.Embedding);
        SES_AI_API_USAGE_HISTORY.setInputCount(new BigDecimal(100));
        SES_AI_API_USAGE_HISTORY.setOutputCount(new BigDecimal(20));
        SES_AI_API_USAGE_HISTORY.save();
    }

    @Test
    void deleteTest() {
        SES_AI_API_USAGE_HISTORY SES_AI_API_USAGE_HISTORY = new SES_AI_API_USAGE_HISTORY();
        SES_AI_API_USAGE_HISTORY.setProvider(Provider.OpenAI);
        SES_AI_API_USAGE_HISTORY.setModel("gpt-4o-mini");
        SES_AI_API_USAGE_HISTORY.setUsageMonth("202501");
        SES_AI_API_USAGE_HISTORY.setUserId("test_user_id");
        SES_AI_API_USAGE_HISTORY.setApiType(ApiType.Generate);
        SES_AI_API_USAGE_HISTORY.delete();
    }

    @Test
    void fetchTest() {
        SES_AI_API_USAGE_HISTORY SES_AI_API_USAGE_HISTORY = new SES_AI_API_USAGE_HISTORY();
        SES_AI_API_USAGE_HISTORY.setProvider(Provider.OpenAI);
        SES_AI_API_USAGE_HISTORY.setModel("gpt-4o-mini");
        SES_AI_API_USAGE_HISTORY.setUsageMonth("202501");
        SES_AI_API_USAGE_HISTORY.setUserId("test_user_id");
        SES_AI_API_USAGE_HISTORY.setApiType(ApiType.Generate);
        SES_AI_API_USAGE_HISTORY.fetch();
        System.out.println(SES_AI_API_USAGE_HISTORY);
    }
}
