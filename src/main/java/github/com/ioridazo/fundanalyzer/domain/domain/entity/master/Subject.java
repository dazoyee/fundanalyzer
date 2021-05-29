package github.com.ioridazo.fundanalyzer.domain.domain.entity.master;

public abstract class Subject {

    private String id;

    private String outlineSubjectId;

    private String detailSubjectId;

    private String name;

    public abstract String getId();

    public abstract String getOutlineSubjectId();

    public abstract String getDetailSubjectId();

    public abstract String getName();
}
