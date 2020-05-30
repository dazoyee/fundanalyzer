package github.com.ioridazo.fundanalyzer.domain.entity.master;

import lombok.Value;
import org.seasar.doma.Entity;
import org.seasar.doma.GeneratedValue;
import org.seasar.doma.GenerationType;
import org.seasar.doma.Id;
import org.seasar.doma.Table;

@SuppressWarnings("RedundantModifiersValueLombok")
@Value
@Entity(immutable = true)
@Table(name = "industry")
public class Industry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private final Integer id;

    private final String name;
}
