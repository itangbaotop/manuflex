package top.itangbao.platform.data.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PageResponseDTO<T> {
    private List<T> content; // 当前页数据
    private int page; // 当前页码
    private int size; // 每页大小
    private long totalElements; // 总记录数
    private int totalPages; // 总页数
    private boolean first; // 是否为第一页
    private boolean last; // 是否为最后一页
}
