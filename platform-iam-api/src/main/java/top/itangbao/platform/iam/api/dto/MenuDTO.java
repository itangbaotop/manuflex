package top.itangbao.platform.iam.api.dto;
import lombok.Data;
import java.util.List;

@Data
public class MenuDTO {
    private Long id;
    private Long parentId;
    private String name;
    private String path;
    private String icon;
    private String permission;
    private Integer sortOrder;
    private Integer type;
    private List<MenuDTO> children;
}