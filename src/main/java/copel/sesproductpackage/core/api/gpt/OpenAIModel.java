package copel.sesproductpackage.core.api.gpt;

/**
 * OpenAIモデルのEnum.
 */
public enum OpenAIModel implements GptModel {
    GPT_3_5_TURBO("gpt-3.5-turbo"),
    GPT_4("gpt-4"),
    GPT_4_TURBO("gpt-4-turbo"),
    TEXT_EMBEDDING_ADA_002("text-embedding-ada-002"); // Common embedding model

    private final String modelName;

    OpenAIModel(String modelName) {
        this.modelName = modelName;
    }

    @Override
    public String getModelName() {
        return modelName;
    }

    /**
     * 指定されたモデル名に対応するOpenAIModelを返します。
     * 見つからない場合はnullを返します。
     *
     * @param modelName モデル名
     * @return OpenAIModelまたはnull
     */
    public static OpenAIModel fromModelName(String modelName) {
        for (OpenAIModel model : OpenAIModel.values()) {
            if (model.getModelName().equals(modelName)) {
                return model;
            }
        }
        return null;
    }
}
