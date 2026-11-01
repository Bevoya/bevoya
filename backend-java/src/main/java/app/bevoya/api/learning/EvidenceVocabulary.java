package app.bevoya.api.learning;

final class EvidenceVocabulary {

  static final String TYPE_PATTERN =
      "note|commit|snippet|project_update|quiz_result|document|link|voice_note|photo|manual";
  static final String LEVEL_PATTERN = "watched|read|practiced|built|tested|applied";
  static final String HTTPS_PATTERN = "https://\\S+";

  private EvidenceVocabulary() {}
}