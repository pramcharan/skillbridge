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
 * Injects a shared responsive layer across all pages.
 * Most pages ship with inline styles, so this central override
 * keeps mobile/tablet behavior consistent without duplicating fixes.
 */
function injectGlobalResponsiveStyles() {
    if (document.getElementById('sb-responsive-patch')) {
        return;
    }

    const style = document.createElement('style');
    style.id = 'sb-responsive-patch';
    style.textContent = `
        html, body {
            max-width: 100%;
            overflow-x: clip;
        }

        img, svg, video, canvas {
            max-width: 100%;
            height: auto;
        }

        .table-wrap {
            overflow-x: auto;
            -webkit-overflow-scrolling: touch;
        }

        table {
            min-width: 640px;
        }

        @media (max-width: 1024px) {
            nav, .navbar {
                padding-left: 16px !important;
                padding-right: 16px !important;
            }

            main, .page-main, .page-layout {
                padding-left: 16px !important;
                padding-right: 16px !important;
            }

            .hero-visual {
                position: static !important;
                transform: none !important;
                width: 100% !important;
                margin-top: 20px;
            }
        }

        @media (max-width: 900px) {
            .page-layout,
            .dashboard-layout,
            .content-grid,
            .split-layout,
            .two-col,
            .form-row {
                grid-template-columns: 1fr !important;
            }

            .stats-grid {
                grid-template-columns: repeat(2, minmax(0, 1fr)) !important;
            }

            .job-row {
                flex-direction: column !important;
                align-items: flex-start !important;
            }

            .job-row > div:last-child {
                width: 100%;
                justify-content: space-between;
                flex-wrap: wrap;
                gap: 10px;
            }
        }

        @media (max-width: 768px) {
            nav, .navbar {
                height: auto !important;
                min-height: 56px;
                flex-wrap: wrap;
                gap: 10px;
                padding-top: 10px;
                padding-bottom: 10px;
            }

            .nav-links {
                display: none !important;
            }

            .nav-actions,
            nav > div {
                width: 100%;
                display: flex;
                justify-content: flex-end;
                flex-wrap: wrap;
                gap: 8px;
            }

            .welcome-banner,
            .section-hdr,
            .toolbar,
            .results-bar,
            .search-bar,
            .jc-top {
                flex-direction: column !important;
                align-items: flex-start !important;
            }

            .btn,
            .btn-hero,
            .submit-btn,
            .btn-search {
                max-width: 100%;
            }

            .modal-box {
                max-width: 100% !important;
                width: calc(100vw - 24px) !important;
                padding: 16px !important;
            }

            #toast-container,
            .toast {
                right: 12px !important;
                left: 12px !important;
                bottom: 12px !important;
                max-width: none !important;
            }

            .tabs {
                overflow-x: auto;
                white-space: nowrap;
                -webkit-overflow-scrolling: touch;
            }
        }

        @media (max-width: 560px) {
            .stats-grid {
                grid-template-columns: 1fr !important;
            }

            .form-card,
            .card,
            .job-card {
                padding: 14px !important;
            }

            .hero-title {
                font-size: clamp(2rem, 11vw, 2.6rem) !important;
                line-height: 1.12;
            }

            .hero-sub {
                font-size: 0.96rem !important;
            }
        }
    `;

    document.head.appendChild(style);
}

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
    injectGlobalResponsiveStyles();
});
