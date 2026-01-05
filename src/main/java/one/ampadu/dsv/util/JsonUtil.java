package one.ampadu.dsv.util;

public class JsonUtil {

    public static String cleanJsonString(String input) {
        int start = input.indexOf("[");
        int end = input.lastIndexOf("]") + 1;

        if (start != -1 && end > start) {
            return input.substring(start, end);
        }
        return input;
    }
}
