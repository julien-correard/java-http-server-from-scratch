// Vibecoded using Opencode

import java.util.Map;
import java.util.HashMap;

public class JsonParser {

  private final String source;
  private int pos;

  public JsonParser(String source) {
    this.source = source;
    this.pos = 0;
  }

  public Map<String, String> parseObject() throws BadRequestException {
    skipWhitespace();
    expect('{');

    Map<String, String> result = new HashMap<>();

    skipWhitespace();
    if (peek() != '}') {
      while (true) {
        skipWhitespace();
        String key = parseString();

        skipWhitespace();
        expect(':');

        skipWhitespace();
        String value = parseString();

        result.put(key, value);

        skipWhitespace();
        char separator = next();
        if (separator == '}') {
          break;
        }
        if (separator != ',') {
          throw new BadRequestException("Attendu ',' ou '}' à la position " + (pos - 1));
        }
      }
    } else {
      next();
    }

    skipWhitespace();
    if (pos < source.length()) {
      throw new BadRequestException("Contenu en trop à la position " + pos);
    }

    return result;
  }

  private String parseString() throws BadRequestException {
    expect('"');

    StringBuilder sb = new StringBuilder();

    while (true) {
      char c = next();
      if (c == '"') {
        return sb.toString();
      }
      if (c == '\\') {
        char escaped = next();
        if (escaped != '"' && escaped != '\\') {
          throw new BadRequestException("Échappement non supporté à la position " + (pos - 1));
        }
        sb.append(escaped);
      } else {
        sb.append(c);
      }
    }
  }

  private void skipWhitespace() {
    while (pos < source.length() && isWhitespace(source.charAt(pos))) {
      pos++;
    }
  }

  private boolean isWhitespace(char c) {
    return c == ' ' || c == '\t' || c == '\n' || c == '\r';
  }

  private char peek() throws BadRequestException {
    if (pos >= source.length()) {
      throw new BadRequestException("Fin de contenu inattendue");
    }
    return source.charAt(pos);
  }

  private char next() throws BadRequestException {
    if (pos >= source.length()) {
      throw new BadRequestException("Fin de contenu inattendue");
    }
    return source.charAt(pos++);
  }

  private void expect(char expected) throws BadRequestException {
    char c = next();
    if (c != expected) {
      throw new BadRequestException(
          "Caractère '" + expected + "' attendu à la position " + (pos - 1)
      );
    }
  }
}
