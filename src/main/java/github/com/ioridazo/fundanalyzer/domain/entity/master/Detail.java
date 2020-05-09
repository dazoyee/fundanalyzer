package github.com.ioridazo.fundanalyzer.domain.entity.master;

public class Detail {

    String id;

    String subjectId;

    String name;

    public Detail(
            String id,
            String subjectId,
            String name) {
        this.id = id;
        this.subjectId = subjectId;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public String getSubjectId() {
        return subjectId;
    }

    public String getName() {
        return name;
    }
}
