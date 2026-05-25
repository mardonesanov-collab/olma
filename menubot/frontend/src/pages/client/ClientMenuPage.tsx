import { useEffect, useMemo, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { useApi } from '../../hooks/useApi';
import { useTelegram } from '../../hooks/useTelegram';
import Toast from '../../components/Toast';

type CartItem = { productId: number; name: string; price: number; quantity: number };

export default function ClientMenuPage() {
    const [params] = useSearchParams();
    const api = useApi();
    const { webApp } = useTelegram();

    const [restaurantId, setRestaurantId] = useState<number | null>(null);
    const [tableNumber, setTableNumber] = useState<string>('');
    const [restaurant, setRestaurant] = useState<any>(null);
    const [menu, setMenu] = useState<any>(null);
    const [search, setSearch] = useState('');
    const [activeCat, setActiveCat] = useState<number | null>(null);
    const [cart, setCart] = useState<CartItem[]>([]);
    const [loading, setLoading] = useState(true);
    const [toast, setToast] = useState<{ msg: string; type: 'success' | 'error' } | null>(null);

    useEffect(() => {
        webApp?.ready?.();
        webApp?.expand?.();
    }, [webApp]);

    useEffect(() => {
        (async () => {
            try {
                const startapp = params.get('tgWebAppStartParam') || params.get('startapp');
                let rid = Number(params.get('restaurantId'));
                let table = params.get('table') || '';

                if (startapp) {
                    const parsed = await api.public.parseStartApp(startapp);
                    rid = parsed.restaurantId;
                    table = parsed.tableNumber;
                }

                setRestaurantId(rid);
                setTableNumber(table);
                const [r, m] = await Promise.all([
                    api.public.restaurant(rid),
                    api.public.menu(rid),
                ]);
                setRestaurant(r);
                setMenu(m);
                if (m.categories?.length) setActiveCat(m.categories[0].id);
            } catch (e: any) {
                setToast({ msg: e.message || 'Xato', type: 'error' });
            } finally {
                setLoading(false);
            }
        })();
    }, []);

    const feePercent = Number(restaurant?.serviceFeePercent || 10);
    const subtotal = cart.reduce((s, i) => s + i.price * i.quantity, 0);
    const serviceFee = Math.round(subtotal * feePercent / 100);
    const total = subtotal + serviceFee;

    const filteredCategories = useMemo(() => {
        if (!menu?.categories) return [];
        if (!search.trim()) return menu.categories;
        const q = search.toLowerCase();
        return menu.categories
            .map((c: any) => ({
                ...c,
                items: c.items.filter((i: any) =>
                    i.name.toLowerCase().includes(q) || (i.description || '').toLowerCase().includes(q)
                ),
            }))
            .filter((c: any) => c.items.length > 0);
    }, [menu, search]);

    function addToCart(item: any) {
        api.haptic('light');
        setCart(prev => {
            const ex = prev.find(p => p.productId === item.id);
            if (ex) return prev.map(p => p.productId === item.id ? { ...p, quantity: p.quantity + 1 } : p);
            return [...prev, { productId: item.id, name: item.name, price: item.price, quantity: 1 }];
        });
    }

    async function submitOrder() {
        if (!restaurantId || cart.length === 0) return;
        try {
            api.haptic('success');
            await api.client.placeOrder(restaurantId, tableNumber || undefined, cart.map(c => ({
                productId: c.productId,
                quantity: c.quantity,
            })));
            setCart([]);
            setToast({ msg: 'Buyurtma qabul qilindi!', type: 'success' });
        } catch (e: any) {
            setToast({ msg: e.message, type: 'error' });
        }
    }

    async function callWaiter() {
        if (!restaurantId) return;
        try {
            api.haptic('medium');
            await api.client.callWaiter(restaurantId, tableNumber || '?');
            setToast({ msg: 'Ofitsiant chaqirildi', type: 'success' });
        } catch (e: any) {
            setToast({ msg: e.message, type: 'error' });
        }
    }

    if (loading) {
        return <div className="public-menu"><div className="skeleton hero-skeleton" /></div>;
    }

    return (
        <div className="public-menu client-app">
            <header className="hero hero--compact">
                <div className="hero-overlay" />
                <div className="hero-content container-wide">
                    <p className="hero-eyebrow">{tableNumber ? `Stol ${tableNumber}` : 'Menyu'}</p>
                    <h1 className="hero-title">{restaurant?.name}</h1>
                </div>
            </header>

            <div className="client-toolbar container-wide">
                <input
                    className="input search-input"
                    placeholder="Taom qidirish..."
                    value={search}
                    onChange={e => setSearch(e.target.value)}
                />
                <button className="btn btn-ghost btn-sm" onClick={callWaiter}>
                    🔔 Ofitsiant
                </button>
            </div>

            {filteredCategories.length > 0 && (
                <nav className="category-nav">
                    <div className="category-nav-inner container-wide">
                        {filteredCategories.map((cat: any) => (
                            <button
                                key={cat.id}
                                className={`category-pill${activeCat === cat.id ? ' active' : ''}`}
                                onClick={() => {
                                    setActiveCat(cat.id);
                                    document.getElementById(`cat-${cat.id}`)?.scrollIntoView({ behavior: 'smooth' });
                                }}
                            >
                                {cat.name}
                            </button>
                        ))}
                    </div>
                </nav>
            )}

            <main className="menu-body container-wide">
                {filteredCategories.map((cat: any) => (
                    <section key={cat.id} id={`cat-${cat.id}`} className="menu-section">
                        <h2 className="section-title">{cat.name}</h2>
                        <div className="menu-grid">
                            {cat.items.map((item: any) => (
                                <article key={item.id} className="menu-item-card" onClick={() => addToCart(item)}>
                                    <div className="menu-item-media">
                                        {item.imageUrl ? (
                                            <img src={item.imageUrl} alt={item.name} />
                                        ) : (
                                            <div className="menu-item-placeholder">🍽</div>
                                        )}
                                    </div>
                                    <div className="menu-item-body">
                                        <div className="menu-item-top">
                                            <h3 className="menu-item-name">{item.name}</h3>
                                            <span className="menu-item-price">
                                                {Number(item.price).toLocaleString()} so'm
                                            </span>
                                        </div>
                                        {item.description && (
                                            <p className="menu-item-desc">{item.description}</p>
                                        )}
                                    </div>
                                </article>
                            ))}
                        </div>
                    </section>
                ))}
            </main>

            {cart.length > 0 && (
                <div className="cart-sheet">
                    <div className="cart-sheet-inner">
                        <h3>Savat</h3>
                        {cart.map(item => (
                            <div key={item.productId} className="cart-line">
                                <span>{item.name} × {item.quantity}</span>
                                <span>{(item.price * item.quantity).toLocaleString()} so'm</span>
                            </div>
                        ))}
                        <div className="cart-totals">
                            <div><span>Taomlar</span><span>{subtotal.toLocaleString()} so'm</span></div>
                            <div><span>Xizmat haqi ({feePercent}%)</span><span>{serviceFee.toLocaleString()} so'm</span></div>
                            <div className="cart-total-final"><span>Jami</span><span>{total.toLocaleString()} so'm</span></div>
                        </div>
                        <button className="btn btn-primary" onClick={submitOrder}>
                            Buyurtma berish
                        </button>
                    </div>
                </div>
            )}

            {toast && <Toast message={toast.msg} type={toast.type} onClose={() => setToast(null)} />}
        </div>
    );
}
