package github.com.ioridazo.fundanalyzer.web.model;

import lombok.Value;

@SuppressWarnings("RedundantModifiersValueLombok")
@Value(staticConstructor = "of")
public class CodeInputData {

    private final String code;
}
