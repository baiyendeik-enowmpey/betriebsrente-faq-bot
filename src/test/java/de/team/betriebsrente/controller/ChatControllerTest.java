package de.team.betriebsrente.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import de.team.betriebsrente.rag.RagService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.ui.ExtendedModelMap;

class ChatControllerTest {

    @Test
    void indexProvidesCategoriesAndChatView() {
        ExtendedModelMap model = new ExtendedModelMap();
        Authentication authentication = mock(Authentication.class);
        doReturn(List.of(new SimpleGrantedAuthority("ROLE_ADMIN"), new SimpleGrantedAuthority("ROLE_USER")))
                .when(authentication).getAuthorities();

        String view = new ChatController(mock(RagService.class)).index(model, authentication);

        assertEquals("chat", view);
        assertEquals(List.of("Auszahlung", "Steuer", "Unverfallbarkeit", "Arbeitgeberwechsel", "Alle"),
                model.getAttribute("kategorien"));
        assertTrue((Boolean) model.getAttribute("isAdmin"));
    }

    @Test
    void indexHidesAdminLinkForRegularUsers() {
        ExtendedModelMap model = new ExtendedModelMap();
        Authentication authentication = mock(Authentication.class);
        doReturn(List.of(new SimpleGrantedAuthority("ROLE_USER"))).when(authentication).getAuthorities();

        new ChatController(mock(RagService.class)).index(model, authentication);

        assertFalse((Boolean) model.getAttribute("isAdmin"));
    }

    @Test
    void chatReturnsAnswerAndTruncatedSourceText() {
        RagService ragService = mock(RagService.class);
        String sourceText = "x".repeat(500);
        Document source = new Document(sourceText, Map.of("titel", "Quelle", "kategorie", "Steuer"));
        when(ragService.ask("Frage", "Steuer")).thenReturn(new RagService.Answer("Antwort", List.of(source)));

        Map<String, Object> result = new ChatController(ragService)
                .chat(Map.of("frage", "Frage", "kategorie", "Steuer"));

        assertEquals("Antwort", result.get("antwort"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> sources = (List<Map<String, Object>>) result.get("quellen");
        assertEquals(1, sources.size());
        assertEquals(403, ((String) sources.get(0).get("text")).length());
        assertEquals("Quelle", sources.get(0).get("titel"));
        assertEquals("Steuer", sources.get(0).get("kategorie"));
    }
}
