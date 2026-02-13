import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class TemporaryAssignment extends AbstractRoleAssignment{
    private String expiresAt;      //дата окончания действия
    private boolean autoRenew;     //автоматическое продление

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public TemporaryAssignment(User user, Role role, AssignmentMetadata metadata, String expiresAt, boolean autoRenew) {
        super(user, role, metadata);
        
        if (expiresAt == null || expiresAt.trim().isEmpty()) {
            throw new IllegalArgumentException("Дата окончания действий не может быть пустой");
        }

        try {
            LocalDate.parse(expiresAt.trim(), FORMATTER);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Неверный формат даты. Используйте: yyyy-MM-dd");
        }

        this.expiresAt = expiresAt.trim();
        this.autoRenew = autoRenew;
    }

    public String getExpiresAt() {
        return expiresAt;
    }

    public boolean isAutoRenew() {
        return autoRenew;
    }

    @Override
    public boolean isActive() {
        try {
            LocalDate today = LocalDate.now();
            LocalDate expiration = LocalDate.parse(expiresAt, FORMATTER);
            return !today.isAfter(expiration);
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isActive(LocalDate currentDate) {
        try {
            LocalDate expiration = LocalDate.parse(expiresAt, FORMATTER);
            return !currentDate.isAfter(expiration);
        } catch (Exception e) {
            return false; 
        }
    }

    //проверка истечения
    public boolean isExpired() {
        return !isActive();
    }

    public void extend(String newExpirationDate) {
        if (newExpirationDate == null || newExpirationDate.trim().isEmpty()) {
            throw new IllegalArgumentException("Новая дата истечения не может быть пустой");
        }

        try {
            LocalDate.parse(newExpirationDate.trim(), FORMATTER);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Неверный формат новой даты: " + newExpirationDate);
        }

        this.expiresAt = newExpirationDate.trim(); //продлеваем
    }

    //строка с отсавшимся временем
    public String getTimeRemaining() {
        if (isExpired()) {
            return "Expired";
        }
        return "Active until: " + expiresAt;
    }
    
    @Override
    public String assignmentType() {
        return "TEMPORARY";
    }

    @Override
    public String summary() {
        String status = isActive() ? "ACTIVE" : "EXPIRED";

        String reasonText = metadata().reason();
        if (reasonText == null || reasonText.trim().isEmpty()) {
            reasonText = "Not specified";
        }

        return String.format(
            "[TEMPORARY] %s assigned to %s by %s at %s%n" +
            "Expires: %s (Auto-renew: %s)%n" +
            "Reason: %s%n" +
            "Status: %s",
            role().getName(),
            user().username(),
            metadata().assignedBy(),
            metadata().assignedAt(),
            expiresAt,
            autoRenew ? "Yes" : "No",
            reasonText,
            status
        );
    }
}
