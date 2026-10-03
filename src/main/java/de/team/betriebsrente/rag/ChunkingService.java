package de.team.betriebsrente.rag;

import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class ChunkingService {
    @Value("${app.chunk.size:650}")
    int chunkSize;
    @Value("${app.chunk.overlap:130}")
    int overlap;

    public List<Document> chunk(String originalText, Map<String, Object> baseMetadata) {
        TokenTextSplitter splitter = new TokenTextSplitter(chunkSize, 350, 5, 10000, true);
        List<Document> docs = splitter.apply(List.of(new Document(originalText, baseMetadata)));
        List<Document> enriched = new ArrayList<>();
        for (int i = 0; i < docs.size(); i++) {
            Document d = docs.get(i);
            Map<String, Object> meta = new HashMap<>(d.getMetadata());
            meta.put("chunk_index", i);
            meta.put("chunk_count", docs.size());
            meta.put("token_estimate", d.getText().length() / 4);
            enriched.add(new Document(d.getText(), meta));
        }
        return enriched;
    }
}
