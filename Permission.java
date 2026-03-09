public record Permission (
    String name,
    String resource,
    String description
) {
    // Пользовательский канонический конструктор
    public Permission {
        ValidationUtils.requireNonNull(name, "Name");
        ValidationUtils.requireNonNull(resource, "Resource");
        ValidationUtils.requireNonNull(description, "Description");
        
        String normName = ValidationUtils.normalizeString(name, true);
        String normResource = ValidationUtils.normalizeString(resource, false);
        String normDescription = ValidationUtils.normalizeString(description);
        
        ValidationUtils.requireNonNullEmpty(normName, "Name");
        ValidationUtils.requireNonNullEmpty(normResource, "Resource");
        ValidationUtils.requireNonNullEmpty(normDescription, "Description");
        
        if (normName.contains(" ")) {
            throw new IllegalArgumentException("Имя права не должно содержать пробелов");
        }
        
        name = normName;
        resource = normResource;
        description = normDescription;
    }
    
    public String format() {
        return String.format("%s on %s: %s", name, resource, description);
    }
    
    public boolean matches(String namePattern, String resourcePattern) {
        boolean nameMatches = true;
        boolean resourceMatches = true;
        
        if (namePattern != null && !namePattern.isEmpty()) {
            nameMatches = this.name.contains(namePattern.toUpperCase());
        }
        
        if (resourcePattern != null && !resourcePattern.isEmpty()) {
            resourceMatches = this.resource.contains(resourcePattern.toLowerCase());
        }
        
        return nameMatches && resourceMatches;
    }
}