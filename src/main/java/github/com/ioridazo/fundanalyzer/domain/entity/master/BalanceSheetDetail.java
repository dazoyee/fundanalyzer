package github.com.ioridazo.fundanalyzer.domain.entity.master;

import org.seasar.doma.Entity;
import org.seasar.doma.Id;
import org.seasar.doma.Table;

@Entity(immutable = true)
@Table(name = "balance_sheet_detail")
public class BalanceSheetDetail extends Detail {

    @Id
    private final String id;

    private final String subjectId;

    private final String name;

    public BalanceSheetDetail(
            String id,
            String subjectId,
            String name) {
        super(id, subjectId, name);
        this.id = id;
        this.subjectId = subjectId;
        this.name = name;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getSubjectId() {
        return subjectId;
    }

    @Override
    public String getName() {
        return name;
    }
}
