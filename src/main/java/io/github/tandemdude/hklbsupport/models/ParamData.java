package io.github.tandemdude.hklbsupport.models;

import java.util.Map;

public record ParamData(Map<String, String> required, Map<String, String> optional) {}
