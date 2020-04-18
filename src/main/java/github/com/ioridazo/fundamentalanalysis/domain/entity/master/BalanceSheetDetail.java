package github.com.ioridazo.fundamentalanalysis.domain.entity.master;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BalanceSheetDetail {

    private String id;

    private String subjectId;

    private String name;
}
