import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public record AssignmentMetadata(
    String assignedBy,  //кто назначил роль
    String assignedAt,  //дата и время назначения
    String reason       //причина
) {
    private static final DateTimeFormatter FORMATTER = 
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public AssignmentMetadata {
        ValidationUtils.requireNonNullEmpty(assignedBy, "Assigned by");
        ValidationUtils.requireNonNullEmpty(assignedAt, "Assigned at");

        assignedBy = ValidationUtils.normalizeString(assignedBy);
        assignedAt = ValidationUtils.normalizeString(assignedAt);
        if (reason != null) {
            reason = ValidationUtils.normalizeString(reason);
            if (reason.isEmpty()) reason = null;
        }
    }

    public static AssignmentMetadata now(String assignedBy, String reason) {
        ValidationUtils.requireNonNullEmpty(assignedBy, "Assigned by");
        
        LocalDateTime now = LocalDateTime.now();
        String formattedDate = now.format(FORMATTER);
        
        return new AssignmentMetadata(assignedBy, formattedDate, reason);
    }

    public String format() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Assigned by: %s%n", assignedBy));
        sb.append(String.format("Assigned at: %s%n", assignedAt));
        
        if (reason != null && !reason.isEmpty()) {
            sb.append(String.format("Reason: %s%n", reason));
        } else {
            sb.append("Reason: Not specified%n");
        }
        
        return sb.toString();
    }
    
    public boolean hasReason() {
        return reason != null && !reason.isEmpty();
    }
    
}
