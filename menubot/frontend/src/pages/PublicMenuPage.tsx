import { useEffect, useRef, useState } from 'react';
import { useParams } from 'react-router-dom';
import { api } from '../api/client';
import type { PublicMenuData } from '../types';

export default function PublicMenuPage() {
    const { uniqueLink } = useParams();
    const [data, setData] = useState<PublicMenuData | null>(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const [activeCat, setActiveCat] = useState<number | null>(null);
    const sectionRefs = useRef<Record<number, HTMLElement | null>>({});

    useEffect(() => {
        if (!uniqueLink) return;
        (async () => {
            try {
                const menu = await api.getPublicMenu(uniqueLink);
                setData(menu);
                if (menu.categories.length > 0) {
                    setActiveCat(menu.categories[0].id);
                }
            } catch {
                setError('Menyu topilmadi yoki vaqtincha mavjud emas.');
            } finally {
                setLoading(false);
            }
        })();
    }, [uniqueLink]);

    useEffect(() => {
        if (!data) return;

        const observer = new IntersectionObserver(
            entries => {
                const visible = entries
                    .filter(e => e.isIntersecting)
                    .sort((a, b) => b.intersectionRatio - a.intersectionRatio)[0];
                if (visible) {
                    const id = Number(visible.target.getAttribute('data-cat-id'));
                    if (id) setActiveCat(id);
                }
            },
            { rootMargin: '-120px 0px -60% 0px', threshold: [0, 0.25, 0.5] }
        );

        data.categories.forEach(cat => {
            const el = sectionRefs.current[cat.id];
            if (el) observer.observe(el);
        });

        return () => observer.disconnect();
    }, [data]);

    function scrollToCategory(catId: number) {
        setActiveCat(catId);
        sectionRefs.current[catId]?.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }

    if (loading) {
        return (
            <div className="public-menu">
                <div className="hero-skeleton skeleton" />
                <div className="container-wide">
                    <div className="nav-pills-skeleton skeleton" />
                    <div className="menu-grid">
                        {[1, 2, 3, 4].map(i => (
                            <div key={i} className="menu-item-card skeleton" style={{ height: 280 }} />
                        ))}
                    </div>
                </div>
            </div>
        );
    }

    if (error || !data) {
        return (
            <div className="public-menu public-menu--error">
                <div className="empty-state">
                    <div className="empty-state-icon">🍽</div>
                    <div className="empty-state-title">Menyu topilmadi</div>
                    <div className="empty-state-text">{error}</div>
                </div>
            </div>
        );
    }

    const { restaurant, categories } = data;
    const totalItems = categories.reduce((sum, c) => sum + c.items.length, 0);

    return (
        <div className="public-menu">
            <header className="hero">
                {restaurant.photoPath ? (
                    <img src={restaurant.photoPath} alt="" className="hero-bg" />
                ) : (
                    <div className="hero-bg hero-bg--placeholder" />
                )}
                <div className="hero-overlay" />
                <div className="hero-content container-wide">
                    <p className="hero-eyebrow">Menyu</p>
                    <h1 className="hero-title">{restaurant.name}</h1>
                    {restaurant.description && (
                        <p className="hero-desc">{restaurant.description}</p>
                    )}
                    <div className="hero-meta">
                        {restaurant.address && (
                            <span className="hero-chip">{restaurant.address}</span>
                        )}
                        {restaurant.phone && (
                            <a href={`tel:${restaurant.phone}`} className="hero-chip hero-chip--link">
                                {restaurant.phone}
                            </a>
                        )}
                        <span className="hero-chip">{totalItems} ta taom</span>
                    </div>
                </div>
            </header>

            {categories.length > 0 && (
                <nav className="category-nav">
                    <div className="category-nav-inner container-wide">
                        {categories.map(cat => (
                            <button
                                key={cat.id}
                                className={`category-pill${activeCat === cat.id ? ' active' : ''}`}
                                onClick={() => scrollToCategory(cat.id)}
                            >
                                {cat.name}
                                <span className="pill-count">{cat.items.length}</span>
                            </button>
                        ))}
                    </div>
                </nav>
            )}

            <main className="menu-body container-wide">
                {categories.length === 0 ? (
                    <div className="empty-state">
                        <div className="empty-state-title">Menyu hali tayyor emas</div>
                        <div className="empty-state-text">Tez orada yangi taomlar qo'shiladi.</div>
                    </div>
                ) : (
                    categories.map((cat, idx) => (
                        <section
                            key={cat.id}
                            id={`cat-${cat.id}`}
                            data-cat-id={cat.id}
                            ref={el => { sectionRefs.current[cat.id] = el; }}
                            className="menu-section"
                            style={{ animationDelay: `${idx * 0.08}s` }}
                        >
                            <div className="section-head">
                                <h2 className="section-title">{cat.name}</h2>
                                <span className="section-count">{cat.items.length} ta</span>
                            </div>

                            {cat.items.length === 0 ? (
                                <p className="section-empty">Bu kategoriyada hozircha taom yo'q.</p>
                            ) : (
                                <div className="menu-grid">
                                    {cat.items.map(item => (
                                        <article key={item.id} className="menu-item-card">
                                            <div className="menu-item-media">
                                                {item.photoPath ? (
                                                    <img src={item.photoPath} alt={item.name} loading="lazy" />
                                                ) : (
                                                    <div className="menu-item-placeholder">🍽</div>
                                                )}
                                            </div>
                                            <div className="menu-item-body">
                                                <div className="menu-item-top">
                                                    <h3 className="menu-item-name">{item.name}</h3>
                                                    {item.price != null && (
                                                        <span className="menu-item-price">
                                                            {Number(item.price).toLocaleString()} so'm
                                                        </span>
                                                    )}
                                                </div>
                                                {item.description && (
                                                    <p className="menu-item-desc">{item.description}</p>
                                                )}
                                            </div>
                                        </article>
                                    ))}
                                </div>
                            )}
                        </section>
                    ))
                )}
            </main>

            <footer className="menu-footer">
                <p>MenuBot · Raqamli menyu</p>
            </footer>
        </div>
    );
}
