package org.markup.poet.theming

/**
 * Registry for managing available themes.
 * 
 * Provides a central location for registering and discovering themes.
 * Supports both built-in and custom themes.
 */
object ThemeRegistry {
    private val themes = mutableMapOf<String, Theme>()
    
    init {
        // Register default theme
        register(DefaultTheme())
    }
    
    /**
     * Registers a theme in the registry.
     * 
     * @param theme The theme to register
     * @throws IllegalArgumentException if a theme with the same ID already exists
     */
    fun register(theme: Theme) {
        if (themes.containsKey(theme.id)) {
            throw IllegalArgumentException("Theme with ID '${theme.id}' is already registered")
        }
        themes[theme.id] = theme
    }
    
    /**
     * Registers a theme, replacing any existing theme with the same ID.
     * 
     * @param theme The theme to register
     */
    fun registerOrReplace(theme: Theme) {
        themes[theme.id] = theme
    }
    
    /**
     * Retrieves a theme by ID.
     * 
     * @param id The theme ID
     * @return The theme, or null if not found
     */
    fun get(id: String): Theme? = themes[id]
    
    /**
     * Retrieves a theme by ID, or returns the default theme if not found.
     * 
     * @param id The theme ID
     * @return The theme, or the default theme if not found
     */
    fun getOrDefault(id: String): Theme = themes[id] ?: DefaultTheme()
    
    /**
     * Returns all registered theme IDs.
     */
    fun listThemes(): List<String> = themes.keys.toList()
    
    /**
     * Returns all registered themes.
     */
    fun getAllThemes(): List<Theme> = themes.values.toList()
    
    /**
     * Unregisters a theme.
     * 
     * @param id The theme ID to unregister
     * @return true if the theme was removed, false if it didn't exist
     */
    fun unregister(id: String): Boolean = themes.remove(id) != null
    
    /**
     * Clears all registered themes except the default theme.
     */
    fun clear() {
        val default = DefaultTheme()
        themes.clear()
        themes[default.id] = default
    }
}
