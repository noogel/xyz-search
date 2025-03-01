package noogel.xyz.search.infrastructure.lucene;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor(staticName = "of")
@NoArgsConstructor
public class HighlightOptions {
    private int fragmentSize;
    private int maxNumFragments;
    private String preTag;
    private String postTag;
}
