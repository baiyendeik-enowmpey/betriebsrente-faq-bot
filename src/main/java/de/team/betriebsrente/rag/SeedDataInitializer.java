package de.team.betriebsrente.rag;

import jakarta.persistence.EntityManager;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.File;

@Configuration
public class SeedDataInitializer {

    @Bean
    public ApplicationRunner seedOnEmpty(IngestionService ingestionService, EntityManager em) {
        return args -> {
            try {
                Long count = (Long) em.createQuery("SELECT COUNT(f) FROM Fachwissen f").getSingleResult();
                if (count == 0) {
                    System.out.println("=== Keine Fachdaten vorhanden - importiere Seed Daten ===");
                    // versuche aus verschiedenen Pfaden
                    String[] paths = {
                            "testdaten/fachwissen_seed.json",
                            "/app/testdaten/fachwissen_seed.json",
                            "src/main/resources/testdaten/fachwissen_seed.json"
                    };
                    for (String p : paths) {
                        try {
                            File f = new ClassPathResource("testdaten/fachwissen_seed.json").getFile();
                            if (f.exists()) {
                                ingestionService.ingestFromJson(f);
                                System.out.println("Seed Import aus ClassPath erfolgreich: " + f.getAbsolutePath());
                                return;
                            }
                        } catch (Exception e) {
                        }
                        try {
                            File f2 = new File(p);
                            if (f2.exists()) {
                                ingestionService.ingestFromJson(f2);
                                System.out.println("Seed Import erfolgreich: " + p);
                                return;
                            }
                        } catch (Exception e) {
                        }
                    }
                    System.out.println("Kein Seed File gefunden - bitte manuell über /admin hochladen");
                } else {
                    System.out.println("Fachwissen vorhanden: " + count + " Einträge");
                }
            } catch (Exception e) {
                System.out.println("Seed Check Fehler: " + e.getMessage());
            }
        };
    }
}
