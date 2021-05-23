package github.com.ioridazo.fundanalyzer.web.model;

import github.com.ioridazo.fundanalyzer.exception.FundanalyzerRuntimeException;
import lombok.Value;

@SuppressWarnings("RedundantModifiersValueLombok")
@Value(staticConstructor = "of")
public class CodeInputData {

    private final String code;

    public String getCode4() {
        if (code.length() == 4) {
            return code;
        } else if (code.length() == 5) {
            return code.substring(0, 4);
        } else {
            throw new FundanalyzerRuntimeException();
        }
    }

    public String getCode5() {
        if (code.length() == 4) {
            return code + "0";
        } else if (code.length() == 5) {
            return code;
        } else {
            throw new FundanalyzerRuntimeException();
        }
    }
}
