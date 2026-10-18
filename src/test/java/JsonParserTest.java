import org.junit.jupiter.api.Test;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class JsonParserTest {

    @Test
    void parseUnObjetSimple() throws Exception {
        // préparer
        JsonParser parser = new JsonParser("{\"name\":\"Alice\",\"phone\":\"0612345678\"}");

        // agir
        Map<String, String> result = parser.parseObject();

        // vérifier
        assertEquals("Alice", result.get("name"));
        assertEquals("0612345678", result.get("phone"));
        assertEquals(2, result.size());
    }

    @Test
    void parseUnObjetVide() throws Exception {
        Map<String, String> result = new JsonParser("{}").parseObject();

        assertTrue(result.isEmpty());
    }

    @Test
    void parseUnGuillemetEchappe() throws Exception {
        // le JSON est : {"name":"a\"b"}   (les \ en Java servent à écrire " et \ dans la chaîne)
        JsonParser parser = new JsonParser("{\"name\":\"a\\\"b\"}");

        assertEquals("a\"b", parser.parseObject().get("name"));
    }

    @Test
    void refuseUnJsonSansAccoladeFermante() {
        JsonParser parser = new JsonParser("{\"name\":\"Alice\"");

        assertThrows(BadRequestException.class, parser::parseObject);
    }

    @Test
    void refuseUneValeurQuiNEstPasUneChaine() {
        JsonParser parser = new JsonParser("{\"phone\":0612345678}");

        assertThrows(BadRequestException.class, parser::parseObject);
    }
}