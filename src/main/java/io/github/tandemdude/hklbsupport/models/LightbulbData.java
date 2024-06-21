package io.github.tandemdude.hklbsupport.models;

import java.util.Map;

public record LightbulbData(String version, Map<String, ParamData> paramData) {}
