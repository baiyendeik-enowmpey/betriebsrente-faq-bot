package de.team.betriebsrente.rag;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.team.betriebsrente.model.Fachwissen;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import java.io.File;
import java.util.*;

@Service
public class IngestionService {
    private final VectorStore vectorStore;
    private final ChunkingService chunkingService;
    private final EntityManager em;
    private final ObjectMapper mapper = new ObjectMapper();

    public IngestionService(VectorStore vectorStore, ChunkingService chunkingService, EntityManager em) {
        this.vectorStore = vectorStore;
        this.chunkingService = chunkingService;
        this.em = em;
    }

    @Transactional
    public void ingestFromJson(File jsonFile) throws Exception {
        List<Map<String, String>> entries = mapper.readValue(jsonFile, new TypeReference<>() {
        });
        for (Map<String, String> e : entries) {
            Fachwissen fw = new Fachwissen();
            fw.setTitel(e.get("titel"));
            fw.setKategorie(e.get("kategorie"));
            fw.setOriginalText(e.get("original_text"));
            fw.setQuelle(e.getOrDefault("quelle", "intern"));
            em.persist(fw);
            em.flush();
            Map<String, Object> meta = new HashMap<>();
            meta.put("fachwissen_id", fw.getId());
            meta.put("titel", fw.getTitel());
            meta.put("kategorie", fw.getKategorie());
            meta.put("quelle", fw.getQuelle());
            List<Document> chunks = chunkingService.chunk(fw.getOriginalText(), meta);
            vectorStore.add(chunks);
        }
    }
}
