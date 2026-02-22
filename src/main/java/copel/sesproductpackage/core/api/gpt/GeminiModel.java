package copel.sesproductpackage.core.api.gpt;

/** GeminiモデルのEnum. */
public enum GeminiModel implements GptModel {
  GEMINI_1_0_PRO("gemini-1.0-pro"),
  GEMINI_1_5_PRO("gemini-1.5-pro"),
  GEMINI_1_5_FLASH("gemini-1.5-flash"),
  GEMINI_1_5_FLASH_8B("gemini-1.5-flash-8b"),
  GEMINI_1_5_FLASH_LITE("gemini-1.5-flash-lite"),
  GEMINI_2_5_PRO("gemini-2.5-pro"),
  GEMINI_2_5_FLASH("gemini-2.5-flash"),
  GEMINI_2_5_FLASH_LITE("gemini-2.5-flash-lite"),
  GEMINI_3_PRO("gemini-3-pro"),
  GEMINI_3_PRO_PREVIEW("gemini-3-pro-preview"),
  GEMINI_3_FLASH("gemini-3-flash"),
  GEMINI_3_FLASH_PREVIEW("gemini-3-flash-preview"),
  GEMINI_EMBEDDING_001("models/gemini-embedding-001");

  private final String modelName;

  GeminiModel(String modelName) {
    this.modelName = modelName;
  }

  @Override
  public String getModelName() {
    return modelName;
  }

  /**
   * 指定されたモデル名に対応するGeminiModelを返します。 見つからない場合はnullを返します。
   *
   * @param modelName モデル名
   * @return GeminiModelまたはnull
   */
  public static GeminiModel fromModelName(String modelName) {
    for (GeminiModel model : GeminiModel.values()) {
      if (model.getModelName().equals(modelName)) {
        return model;
      }
    }
    return null;
  }
}
