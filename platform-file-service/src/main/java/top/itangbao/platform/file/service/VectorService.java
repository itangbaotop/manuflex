package top.itangbao.platform.file.service;

import top.itangbao.platform.file.dto.SearchResult;

import java.util.*;

public interface VectorService {

    String upsertDocument(Long docId, String text, Map<String, Object> metadata);

    List<SearchResult> search(String query, int limit);

}