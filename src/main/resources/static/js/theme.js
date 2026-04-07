/* 
 * SkillBridge — Theme System 
 * Handles persistence and switching between Dark/Light modes.
 */
(function() {
    // Apply theme immediately to prevent FOUC (Flash of Unstyled Content)
    const savedTheme = localStorage.getItem('sb_theme') || 'dark';
    document.documentElement.setAttribute('data-theme', savedTheme);
})();

/**
 * Toggles the global theme and persists it to localStorage.
 */
function toggleTheme() {
    const currentTheme = document.documentElement.getAttribute('data-theme') || 'dark';
    const newTheme = currentTheme === 'dark' ? 'light' : 'dark';
    
    document.documentElement.setAttribute('data-theme', newTheme);
    localStorage.setItem('sb_theme', newTheme);
    
    // Update any toggle icons on the page
    updateThemeIcons(newTheme);
}

/**
 * Updates all theme toggle buttons on the page to reflect current state.
 */
function updateThemeIcons(theme) {
    const icons = document.querySelectorAll('.theme-toggle-icon');
    icons.forEach(icon => {
        // Sun (light) or Moon (dark) icon
        if (theme === 'light') {
            icon.innerHTML = '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="5"/><line x1="12" y1="1" x2="12" y2="3"/><line x1="12" y1="21" x2="12" y2="23"/><line x1="4.22" y1="4.22" x2="5.64" y2="5.64"/><line x1="18.36" y1="18.36" x2="19.78" y2="19.78"/><line x1="1" y1="12" x2="3" y2="12"/><line x1="21" y1="12" x2="23" y2="12"/><line x1="4.22" y1="19.78" x2="5.64" y2="18.36"/><line x1="18.36" y1="5.64" x2="19.78" y2="4.22"/></svg>';
            icon.title = 'Switch to Dark Mode';
        } else {
            icon.innerHTML = '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"/></svg>';
            icon.title = 'Switch to Light Mode';
        }
    });
}

// Sync theme changes across multiple open tabs
window.addEventListener('storage', (e) => {
    if (e.key === 'sb_theme') {
        const theme = e.newValue || 'dark';
        document.documentElement.setAttribute('data-theme', theme);
        updateThemeIcons(theme);
    }
});

// Initialize icons on page load
document.addEventListener('DOMContentLoaded', () => {
    const currentTheme = document.documentElement.getAttribute('data-theme') || 'dark';
    updateThemeIcons(currentTheme);
});
