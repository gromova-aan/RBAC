public record Permission (
    String name,
    String resource,
    String description
) {
    // Пользовательский канонический конструктор
    public Permission {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Имя права не может быть пустым");
        }
        
        name = name.trim().toUpperCase();

        if (name.contains(" ")) {
            throw new IllegalArgumentException("Имя права не должно содержать пробелов");
        }
        
        if (resource == null || resource.trim().isEmpty()) {
            throw new IllegalArgumentException("Ресурс не может быть пустым");
        }
        resource = resource.trim().toLowerCase();
        
        if (description == null || description.trim().isEmpty()) {
            throw new IllegalArgumentException("Описание не может быть пустым");
        }
        description = description.trim();
    }

    public String format() {
        return String.format("%s on %s: %s", name, resource, description);
    }

    public boolean matches(String namePattern, String resourcePattern) {
        boolean nameMatches = true;
        boolean resourceMatches = true;
        
        // Если шаблон не null и не пустой, проверяем содержит ли поле этот шаблон
        if (namePattern != null && !namePattern.isEmpty()) {
            nameMatches = this.name.contains(namePattern.toUpperCase());
        }
        
        if (resourcePattern != null && !resourcePattern.isEmpty()) {
            resourceMatches = this.resource.contains(resourcePattern.toLowerCase());
        }
        
        return nameMatches && resourceMatches;
    }
}