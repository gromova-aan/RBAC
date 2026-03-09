import java.util.List;

public class FormatUtils {
    
    public static String formatTable(String[] headers, List<String[]> rows) {
        if (headers == null || headers.length == 0) {
            return "";
        }
        
        // Определяем ширину каждого столбца
        int[] colWidths = new int[headers.length];
        for (int i = 0; i < headers.length; i++) {
            colWidths[i] = headers[i].length();
        }
        
        // Учитываем данные в строках
        for (String[] row : rows) {
            for (int i = 0; i < Math.min(row.length, headers.length); i++) {
                if (row[i] != null && row[i].length() > colWidths[i]) {
                    colWidths[i] = row[i].length();
                }
            }
        }
        
        // Добавляем отступы по 2 пробела с каждой стороны
        for (int i = 0; i < colWidths.length; i++) {
            colWidths[i] += 4;
        }
        
        StringBuilder sb = new StringBuilder();
        
        // Верхняя граница
        sb.append("+");
        for (int i = 0; i < colWidths.length; i++) {
            sb.append("-".repeat(colWidths[i]));
            if (i < colWidths.length - 1) {
                sb.append("+");
            }
        }
        sb.append("+\n");
        
        // Заголовки
        sb.append("|");
        for (int i = 0; i < headers.length; i++) {
            sb.append(padCenter(headers[i], colWidths[i]));
            sb.append("|");
        }
        sb.append("\n");
        
        // Разделитель под заголовками
        sb.append("+");
        for (int i = 0; i < colWidths.length; i++) {
            sb.append("-".repeat(colWidths[i]));
            if (i < colWidths.length - 1) {
                sb.append("+");
            }
        }
        sb.append("+\n");
        
        // Строки данных
        for (String[] row : rows) {
            sb.append("|");
            for (int i = 0; i < headers.length; i++) {
                String value = (i < row.length && row[i] != null) ? row[i] : "";
                sb.append(padCenter(value, colWidths[i]));
                sb.append("|");
            }
            sb.append("\n");
        }
        
        // Нижняя граница
        sb.append("+");
        for (int i = 0; i < colWidths.length; i++) {
            sb.append("-".repeat(colWidths[i]));
            if (i < colWidths.length - 1) {
                sb.append("+");
            }
        }
        sb.append("+\n");
        
        return sb.toString();
    }
    
    public static String formatBox(String text) {
        if (text == null || text.isEmpty()) {
            return "+--+\n│  │\n+--+";
        }
        
        String[] lines = text.split("\n");
        int maxLength = 0;
        for (String line : lines) {
            maxLength = Math.max(maxLength, line.length());
        }
        
        int width = maxLength + 4;
        StringBuilder sb = new StringBuilder();
        
        // Верхняя граница
        sb.append("+").append("-".repeat(width - 2)).append("+\n");
        
        // Текст
        for (String line : lines) {
            sb.append("| ").append(padRight(line, maxLength)).append(" |\n");
        }
        
        // Нижняя граница
        sb.append("+").append("-".repeat(width - 2)).append("+\n");
        
        return sb.toString();
    }
    
    public static String formatHeader(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        
        String line = "-".repeat(text.length() + 4);
        return String.format("+%s+\n|  %s  |\n+%s+", line, text, line);
    }
    
    // Обрезает длинную строку с добавлением "..."
    public static String truncate(String text, int maxLength) {
        if (text == null) return "";
        if (text.length() <= maxLength) return text;
        if (maxLength <= 3) return ".".repeat(maxLength);
        return text.substring(0, maxLength - 3) + "...";
    }
    
    //Дополняет строку пробелами справа до указанной длины
    public static String padRight(String text, int length) {
        if (text == null) text = "";
        if (text.length() >= length) return text;
        return text + " ".repeat(length - text.length());
    }
    
    //Дополняет строку пробелами слева до указанной длины
    public static String padLeft(String text, int length) {
        if (text == null) text = "";
        if (text.length() >= length) return text;
        return " ".repeat(length - text.length()) + text;
    }
    
    //Дополняет строку пробелами слева и справа для центрирования
    private static String padCenter(String text, int length) {
        if (text == null) text = "";
        if (text.length() >= length) return text;
        
        int spaces = length - text.length();
        int leftSpaces = spaces / 2;
        int rightSpaces = spaces - leftSpaces;
        
        return " ".repeat(leftSpaces) + text + " ".repeat(rightSpaces);
    }
}