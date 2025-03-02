package noogel.xyz.search.infrastructure.lucene;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.lucene.search.SortField;

@Data
@AllArgsConstructor(staticName = "of")
@NoArgsConstructor
public class OrderBy {
    private String field;
    private SortField.Type type;
    private boolean asc;
}
