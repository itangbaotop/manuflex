package top.itangbao.platform.file.dto;

import lombok.Data;

import java.util.Map;

@Data
public class SearchResult {

    public Long id;
    public double score;
    public Map<String, Object> metadata;

    public SearchResult(Long id, double score, Map<String, Object> metadata) {
        this.id = id;
        this.score = score;
        this.metadata = metadata;
    }

}
