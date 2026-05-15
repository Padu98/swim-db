package one.ampadu.dsv.util;

public class JsonUtil {

    /**
     * Escapes unescaped double quotes inside "club" string values produced by the LLM.
     * Club names like S.C."Hellas" Einbeck e.V. break JSON parsing; this fixes them by
     * treating the last `"` on each club line as the true closing quote and escaping the rest.
     */
    public static String fixClubFieldQuotes(String json) {
        StringBuilder sb = new StringBuilder(json);
        String prefix = "\"club\": \"";
        int pos = 0;

        while (pos < sb.length()) {
            int clubIdx = sb.indexOf(prefix, pos);
            if (clubIdx == -1) break;

            int valueStart = clubIdx + prefix.length();
            int lineEnd = sb.indexOf("\n", valueStart);
            if (lineEnd == -1) lineEnd = sb.length();

            // Find the last `"` on this line — that is the true closing quote
            int valueEnd = -1;
            for (int i = lineEnd - 1; i >= valueStart; i--) {
                if (sb.charAt(i) == '"') {
                    valueEnd = i;
                    break;
                }
            }

            if (valueEnd == -1 || valueEnd == valueStart) {
                pos = lineEnd;
                continue;
            }

            String value = sb.substring(valueStart, valueEnd);
            if (value.contains("\"")) {
                String fixed = value.replace("\"", "\\\"");
                sb.replace(valueStart, valueEnd, fixed);
                pos = valueStart + fixed.length() + 1;
            } else {
                pos = valueEnd + 1;
            }
        }

        return sb.toString();
    }

    public static String cleanJsonArrayString(String input) {
        int start = input.indexOf("[");
        int end = input.lastIndexOf("]") + 1;

        if (start != -1 && end > start) {
            return input.substring(start, end);
        }
        return input;
    }

    public static String cleanMetaDataJson(String input) {
        int start = input.indexOf("{");
        int end = input.lastIndexOf("}") + 1;

        if (start != -1 && end > start) {
            return input.substring(start, end);
        }
        return input;
    }
}
