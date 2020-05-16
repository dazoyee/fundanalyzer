package github.com.ioridazo.fundanalyzer.domain.entity.master;

public abstract class Detail {

    String id;

    String outlineSubjectId;

    String detailSubjectId;

    String name;

    public abstract String getId();

    public abstract String getOutlineSubjectId();

    public abstract String getDetailSubjectId();

    public abstract String getName();
}
