package top.itangbao.platform.file.service.impl;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.googleai.GoogleAiEmbeddingModel;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.Collections.*;
import io.qdrant.client.grpc.Points.*;
import io.qdrant.client.grpc.JsonWithInt.Value;
import static io.qdrant.client.grpc.Points.UpsertPoints;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import top.itangbao.platform.file.dto.SearchResult;
import top.itangbao.platform.file.service.VectorService;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class VectorServiceImpl implements VectorService {

    private final QdrantClient qdrantClient;
    private final EmbeddingModel embeddingModel;
    private final String collectionName;

    public VectorServiceImpl(
            QdrantClient qdrantClient,
            @org.springframework.beans.factory.annotation.Value("${gemini.api-key}") String apiKey,
            @org.springframework.beans.factory.annotation.Value("${gemini.embedding-model}") String embeddingModel,
            @org.springframework.beans.factory.annotation.Value("${qdrant.collection-name:knowledge_768}") String collectionName) {
        this.qdrantClient = qdrantClient;
        this.embeddingModel = GoogleAiEmbeddingModel.builder()
                .apiKey(apiKey)
                .modelName(embeddingModel)
                .build();
        this.collectionName = collectionName;
        initializeCollection();
    }
    
    private void initializeCollection() {
        try {
            List<String> response = qdrantClient.listCollectionsAsync().get();
            boolean exists = response.contains(collectionName);
            if (!exists) {
                VectorParams vectorParams = VectorParams.newBuilder()
                        .setSize(768)
                        .setDistance(Distance.Cosine)
                        .build();
                
                CreateCollection createCollection = CreateCollection.newBuilder()
                        .setCollectionName(collectionName)
                        .setVectorsConfig(VectorsConfig.newBuilder()
                                .setParams(vectorParams)
                                .build())
                        .build();
                
                qdrantClient.createCollectionAsync(createCollection).get();
                log.info("Created Qdrant collection: {}", collectionName);
            }
        } catch (Exception e) {
            log.error("Failed to initialize collection", e);
        }
    }

    @Override
    public String upsertDocument(Long docId, String text, Map<String, Object> metadata) {
        try {
            Embedding embedding = embeddingModel.embed(text).content();
            List<Float> vector = embedding.vectorAsList();
            
            Vector.Builder vectorBuilder = Vector.newBuilder();
            for (Float value : vector) {
                vectorBuilder.addData(value);
            }
            
            PointStruct point = PointStruct.newBuilder()
                    .setId(PointId.newBuilder().setNum(docId).build())
                    .setVectors(Vectors.newBuilder()
                            .setVector(vectorBuilder.build())
                            .build())
                    .putAllPayload(convertMetadata(metadata))
                    .build();
            
            qdrantClient.upsertAsync(UpsertPoints.newBuilder()
                    .setCollectionName(collectionName)
                    .addAllPoints(Collections.singletonList(point))
                    .build()).get();
            return "success";
        } catch (Exception e) {
            log.error("Failed to upsert document", e);
            return "failed";
        }
    }

    @Override
    public List<SearchResult> search(String query, int limit) {
        try {
            Embedding queryEmbedding = embeddingModel.embed(query).content();
            List<Float> queryVector = queryEmbedding.vectorAsList();
            
            SearchPoints searchPoints = SearchPoints.newBuilder()
                    .setCollectionName(collectionName)
                    .addAllVector(queryVector)
                    .setLimit(limit)
                    .setWithPayload(WithPayloadSelector.newBuilder().setEnable(true).build())
                    .build();
            
            List<ScoredPoint> results = qdrantClient.searchAsync(searchPoints).get();
            
            return results.stream()
                    .map(point -> new SearchResult(
                            point.getId().getNum(),
                            point.getScore(),
                            extractPayload(point.getPayloadMap())
                    ))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Failed to search", e);
            return Collections.emptyList();
        }
    }

    private Map<String, Value> convertMetadata(Map<String, Object> metadata) {
        Map<String, Value> payload = new HashMap<>();
        metadata.forEach((key, value) -> {
            if (value instanceof String) {
                payload.put(key, Value.newBuilder().setStringValue((String) value).build());
            }
        });
        return payload;
    }
    
    private Map<String, Object> extractPayload(Map<String, Value> payloadMap) {
        Map<String, Object> result = new HashMap<>();
        payloadMap.forEach((key, value) -> {
            if (value.hasStringValue()) {
                result.put(key, value.getStringValue());
            }
        });
        return result;
    }

}