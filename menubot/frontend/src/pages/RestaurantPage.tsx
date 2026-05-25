import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { api } from '../api/client';
import { useTelegram } from '../hooks/useTelegram';
import AddItemForm from '../components/AddItemForm';
import Toast from '../components/Toast';

export default function RestaurantPage() {
    const { userId: paramUserId, restId } = useParams();
    const { userId: tgUserId, webApp } = useTelegram();
    const navigate = useNavigate();

    const userId = tgUserId || Number(paramUserId);

    const [categories, setCategories] = useState<any[]>([]);
    const [activeCat, setActiveCat] = useState<number | null>(null);
    const [editingItem, setEditingItem] = useState<any | null>(null);
    const [showAddCat, setShowAddCat] = useState(false);
    const [newCatName, setNewCatName] = useState('');
    const [addingCat, setAddingCat] = useState(false);
    const [loading, setLoading] = useState(true);
    const [toast, setToast] = useState<{ msg: string; type: 'success' | 'error' } | null>(null);

    const showToast = (msg: string, type: 'success' | 'error') => setToast({ msg, type });

    const loadData = async () => {
        if (!restId) return;
        setLoading(true);
        try {
            const cats = await api.getCategories(Number(restId));
            const detailedCats = await Promise.all(cats.map(async (c: any) => ({
                ...c,
                items: await api.getItems(c.id),
            })));
            setCategories(detailedCats);
        } catch (e) {
            console.error(e);
            showToast("Ma'lumotlarni yuklashda xato", 'error');
        } finally {
            setLoading(false);
        }
    };

    async function handleDeleteItem(itemId: number) {
        if (!confirm("Ushbu taomni o'chirishni xohlaysizmi?")) return;
        try {
            await api.deleteItem(itemId, userId!);
            showToast("Taom o'chirildi", 'success');
            loadData();
        } catch {
            showToast('Xatolik yuz berdi', 'error');
        }
    }

    async function handleDeleteCategory(catId: number) {
        if (!confirm("Kategoriya va undagi barcha taomlar o'chiriladi. Davom etasizmi?")) return;
        try {
            await api.deleteCategory(catId, userId!);
            showToast("Kategoriya o'chirildi", 'success');
            loadData();
        } catch {
            showToast('Xatolik yuz berdi', 'error');
        }
    }

    async function handleToggleAvailable(item: any) {
        try {
            const fd = new FormData();
            fd.append('userId', String(userId));
            fd.append('available', String(!item.available));
            await api.updateItem(item.id, fd);
            showToast(item.available ? 'Taom yashirildi' : "Taom faollashtirildi", 'success');
            loadData();
        } catch {
            showToast('Xatolik yuz berdi', 'error');
        }
    }

    async function handleAddCategory(e: React.FormEvent) {
        e.preventDefault();
        if (!newCatName.trim() || !restId) return;
        setAddingCat(true);
        try {
            await api.addCategory(userId!, Number(restId), newCatName.trim());
            setNewCatName('');
            setShowAddCat(false);
            showToast("Kategoriya qo'shildi", 'success');
            loadData();
        } catch {
            showToast('Xatolik yuz berdi', 'error');
        } finally {
            setAddingCat(false);
        }
    }

    async function handleCopyMenuLink() {
        if (!restId) return;
        try {
            const data = await api.getMenuLink(Number(restId), userId!);
            const link = data.link as string;
            if (webApp?.openLink) {
                webApp.openLink(`https://t.me/share/url?url=${encodeURIComponent(link)}`);
            } else {
                await navigator.clipboard.writeText(link);
                showToast('Menyu havolasi nusxalandi', 'success');
            }
        } catch {
            showToast('Havolani olishda xato', 'error');
        }
    }

    useEffect(() => { loadData(); }, [restId]);

    if (loading) {
        return (
            <>
                <div className="skeleton" style={{ height: 36, width: 120, marginBottom: 20 }} />
                <div className="skeleton skeleton-card" />
                <div className="skeleton skeleton-card" />
            </>
        );
    }

    return (
        <>
            <button className="back-btn" onClick={() => navigate(-1)}>
                ← Ortga
            </button>

            <header className="page-header">
                <div>
                    <h1 className="page-title">Menyu</h1>
                    <p className="page-subtitle">{categories.length} ta kategoriya</p>
                </div>
                {categories.length > 0 && (
                    <button className="btn btn-ghost" onClick={handleCopyMenuLink}>
                        Havola
                    </button>
                )}
            </header>

            {categories.length === 0 && !showAddCat ? (
                <div className="empty-state">
                    <div className="empty-state-icon">📭</div>
                    <div className="empty-state-title">Menyu bo'sh</div>
                    <div className="empty-state-text">
                        Birinchi kategoriyani qo'shing, so'ng taomlarni boshqaring.
                    </div>
                    <button className="btn btn-primary" style={{ maxWidth: 280, margin: '0 auto' }} onClick={() => setShowAddCat(true)}>
                        Kategoriya qo'shish
                    </button>
                </div>
            ) : (
                categories.map((cat, idx) => (
                    <article key={cat.id} className="card" style={{ animationDelay: `${idx * 0.08}s` }}>
                        <div className="category-header">
                            <div className="category-title">
                                <div className="category-emoji">🍴</div>
                                <span>{cat.name}</span>
                            </div>
                            <div className="category-actions">
                                <span className="category-count">{cat.items.length} ta</span>
                                <button
                                    className="icon-btn icon-btn--danger"
                                    onClick={() => handleDeleteCategory(cat.id)}
                                    title="Kategoriyani o'chirish"
                                >
                                    🗑
                                </button>
                            </div>
                        </div>

                        {cat.items.length === 0 ? (
                            <p style={{ padding: '16px 0', textAlign: 'center', color: 'var(--tg-hint)', fontSize: 14 }}>
                                Ushbu kategoriyada taom yo'q
                            </p>
                        ) : (
                            cat.items.map((item: any) => (
                                <div
                                    key={item.id}
                                    className={`item-row${item.available === false ? ' item-row--unavailable' : ''}`}
                                >
                                    {item.photoPath ? (
                                        <img src={item.photoPath} alt={item.name} className="item-image" />
                                    ) : (
                                        <div className="item-image">🍽</div>
                                    )}

                                    <div className="item-info">
                                        <div className="item-name">{item.name}</div>
                                        {item.description && (
                                            <div className="item-description">{item.description}</div>
                                        )}
                                        {item.available === false && (
                                            <span className="badge badge-muted" style={{ marginTop: 4 }}>Yashirin</span>
                                        )}
                                    </div>

                                    {item.price && (
                                        <div className="item-price">
                                            {Number(item.price).toLocaleString()} so'm
                                        </div>
                                    )}

                                    <div className="item-actions">
                                        <button
                                            className="icon-btn"
                                            onClick={() => handleToggleAvailable(item)}
                                            title={item.available !== false ? 'Yashirish' : 'Ko\'rsatish'}
                                        >
                                            {item.available !== false ? '👁' : '🚫'}
                                        </button>
                                        <button
                                            className="icon-btn"
                                            onClick={() => {
                                                setActiveCat(null);
                                                setEditingItem(item);
                                            }}
                                            title="Tahrirlash"
                                        >
                                            ✏️
                                        </button>
                                        <button
                                            className="icon-btn icon-btn--danger"
                                            onClick={() => handleDeleteItem(item.id)}
                                            title="O'chirish"
                                        >
                                            🗑
                                        </button>
                                    </div>
                                </div>
                            ))
                        )}

                        {editingItem?.categoryId === cat.id ? (
                            <AddItemForm
                                userId={userId!}
                                restaurantId={Number(restId)}
                                categoryId={cat.id}
                                item={editingItem}
                                onSuccess={() => {
                                    setEditingItem(null);
                                    showToast('Taom yangilandi', 'success');
                                    loadData();
                                }}
                                onCancel={() => setEditingItem(null)}
                                onError={msg => showToast(msg, 'error')}
                            />
                        ) : activeCat === cat.id ? (
                            <AddItemForm
                                userId={userId!}
                                restaurantId={Number(restId)}
                                categoryId={cat.id}
                                onSuccess={() => {
                                    setActiveCat(null);
                                    showToast("Taom qo'shildi", 'success');
                                    loadData();
                                }}
                                onCancel={() => setActiveCat(null)}
                                onError={msg => showToast(msg, 'error')}
                            />
                        ) : (
                            <button
                                className="btn btn-ghost"
                                style={{ marginTop: 12, width: '100%' }}
                                onClick={() => {
                                    setEditingItem(null);
                                    setActiveCat(cat.id);
                                }}
                            >
                                Yangi taom qo'shish
                            </button>
                        )}
                    </article>
                ))
            )}

            {showAddCat && (
                <div className="card">
                    <form onSubmit={handleAddCategory} className="form">
                        <div className="input-group">
                            <span className="input-icon">📂</span>
                            <input
                                className="input input-with-icon"
                                placeholder="Kategoriya nomi *"
                                value={newCatName}
                                onChange={e => setNewCatName(e.target.value)}
                                autoFocus
                            />
                        </div>
                        <div className="form-row">
                            <button
                                className="btn btn-secondary"
                                type="button"
                                onClick={() => { setShowAddCat(false); setNewCatName(''); }}
                            >
                                Bekor qilish
                            </button>
                            <button className="btn btn-primary" type="submit" disabled={addingCat || !newCatName.trim()}>
                                {addingCat ? 'Saqlanmoqda...' : 'Saqlash'}
                            </button>
                        </div>
                    </form>
                </div>
            )}

            {categories.length > 0 && !showAddCat && (
                <button className="btn btn-ghost" style={{ width: '100%' }} onClick={() => setShowAddCat(true)}>
                    Kategoriya qo'shish
                </button>
            )}

            {toast && (
                <Toast message={toast.msg} type={toast.type} onClose={() => setToast(null)} />
            )}
        </>
    );
}
