package github.com.ioridazo.fundanalyzer.domain.bean;

import lombok.Value;
import org.seasar.doma.Column;
import org.seasar.doma.Entity;
import org.seasar.doma.Id;
import org.seasar.doma.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;

@SuppressWarnings("RedundantModifiersValueLombok")
@Value
@Entity(immutable = true)
@Table(name = "edinet_list_view")
public class EdinetListViewBean {

    // 提出日
    @Id
    private final LocalDate submitDate;

    // 総件数
    private final Long countAll;

    // 処理対象件数
    private final Long countTarget;

    // 処理済件数
    private final Long countScraped;

    // 分析済件数
    private final Long countAnalyzed;

    // 未分析コード
    private final String notAnalyzedCode;

    // 処理確認コード
    private final String cantScrapedCode;

    // 未処理件数
    private final Long countNotScraped;

    // 対象外件数
    private final Long countNotTarget;

    @Column(updatable = false)
    private final LocalDateTime createdAt;

    private final LocalDateTime updatedAt;
}
