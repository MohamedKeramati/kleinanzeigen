package de.example.kleinanzeigenfilter

object WebFilterInjector {
    fun buildInjectionScript(words: Set<String>): String {
        val safeWords = words.map { it.replace("\\", "\\\\").replace("\"", "\\\"") }
        val wordsArray = safeWords.joinToString(prefix = "[\"", separator = "\",\"", postfix = "\"]")
        return """
            (function() {
                try {
                    const blockedWords = $wordsArray.map(w => (w || '').toLowerCase());
                    if (!window.__kzFilterStats) {
                        window.__kzFilterStats = { hiddenImages: 0 };
                    }

                    const normalize = (value) => (value || '').toLowerCase();
                    const hasBlockedWord = (text) => {
                        const t = normalize(text);
                        return blockedWords.some(w => w && t.includes(w));
                    };

                    const hideImagesInside = (container) => {
                        if (!container) return;
                        const imgs = container.querySelectorAll('img');
                        imgs.forEach(img => {
                            if (img && img.style.display !== 'none') {
                                img.style.display = 'none';
                                window.__kzFilterStats.hiddenImages += 1;
                            }
                        });
                    };

                    const scanListItems = () => {
                        const items = document.querySelectorAll('article, li, [data-testid*="ad"], [class*="aditem"], [class*="gallery"]');
                        items.forEach(item => {
                            const titleEl = item.querySelector('h1, h2, h3, [data-testid*="title"], a[title], .text-module-begin');
                            const title = titleEl ? (titleEl.innerText || titleEl.textContent || '') : (item.innerText || '');
                            if (hasBlockedWord(title)) {
                                hideImagesInside(item);
                            }
                        });
                    };

                    const scanDetailPage = () => {
                        const title = (document.querySelector('h1, h2')?.innerText) || '';
                        const desc = (document.querySelector('[data-testid*="description"], [class*="description"], #viewad-description-text')?.innerText) || '';
                        if (hasBlockedWord(title + ' ' + desc)) {
                            hideImagesInside(document.body);
                        }
                    };

                    const runScan = () => {
                        try {
                            scanListItems();
                            scanDetailPage();
                        } catch (_) {}
                    };

                    if (window.__kzFilterObserver) {
                        try { window.__kzFilterObserver.disconnect(); } catch (_) {}
                    }
                    const observer = new MutationObserver(() => { runScan(); });
                    observer.observe(document.documentElement || document.body, { childList: true, subtree: true });
                    window.__kzFilterObserver = observer;

                    if (window.__kzFilterInterval) {
                        clearInterval(window.__kzFilterInterval);
                    }
                    window.__kzFilterInterval = setInterval(runScan, 2500);

                    runScan();
                } catch (_) {}
            })();
        """.trimIndent()
    }
}
