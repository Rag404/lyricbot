package dataobjects;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class Request
{
    private Long userId;
    private String link;
    private String title;
    private int season;
}
