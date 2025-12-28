package copel.sesproductpackage.core.database;

import org.junit.jupiter.api.Test;

import copel.sesproductpackage.core.database.SES_AI_API_USAGE_HISTORY.Provider;

class SES_AI_API_USAGE_HISTORYLotTests {

    @Test
    void fetchByProviderTest() {
        SES_AI_API_USAGE_HISTORYLot entityLot = new SES_AI_API_USAGE_HISTORYLot();
        entityLot.fetchByProvider(Provider.OpenAI);
        System.out.println(entityLot);
    }

    @Test
    void getSumInputCountTest() {
        SES_AI_API_USAGE_HISTORYLot entityLot = new SES_AI_API_USAGE_HISTORYLot();
        entityLot.fetchByProvider(Provider.OpenAI);
        System.out.println(entityLot.getSumInputCount());
    }

    @Test
    void fetchByPkTest() {
        SES_AI_API_USAGE_HISTORY searchCondition = new SES_AI_API_USAGE_HISTORY();
        searchCondition.setProvider(Provider.OpenAI);
        searchCondition.setModel("gpt-4o-mini");
        searchCondition.setUsageMonth("202501");
        SES_AI_API_USAGE_HISTORYLot entityLot = new SES_AI_API_USAGE_HISTORYLot();
        entityLot.fetchByPk(searchCondition.getPartitionKey());
        System.out.println(entityLot);
    }
}
