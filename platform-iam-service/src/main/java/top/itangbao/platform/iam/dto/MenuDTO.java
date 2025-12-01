package top.itangbao.platform.iam.dto;
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
    private List<MenuDTO> children;
}