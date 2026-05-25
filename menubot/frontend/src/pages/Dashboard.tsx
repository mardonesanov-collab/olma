import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '../api/client';
import { useTelegram } from '../hooks/useTelegram';

export default function Dashboard() {
    const { userId, ready } = useTelegram();
    const navigate = useNavigate();
    const [list, setList] = useState<any[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        if (!ready) return;

        if (!userId) {
            setError('Foydalanuvchi ID aniqlanmadi. Iltimos, Telegram bot orqali oching.');
            setLoading(false);
            return;
        }

        (async () => {
            try {
                const status = await api.checkUser(userId);
                if (!status.exists) {
                    setError("Siz ro'yxatdan o'tmagansiz. Botda /start bosing.");
                    setLoading(false);
                    return;
                }
                if (!status.approved) {
                    setError(status.role === 'CLIENT'
                        ? "Vendor sifatida ro'yxatdan o'ting: Telegram botda /start bosing, keyin restoran qo'shing."
                        : "Hisobingiz hali tasdiqlanmagan. Admin tasdiqlashini kuting.");
                    setLoading(false);
                    return;
                }
                const data = await api.getRestaurants(userId);
                setList(data);
            } catch (e: any) {
                setError(e.message || "Xato yuz berdi");
            } finally {
                setLoading(false);
            }
        })();
    }, [userId, ready]);

    if (!ready || loading) {
        return (
            <>
                <div className="skeleton skeleton-card" />
                <div className="skeleton skeleton-card" />
            </>
        );
    }

    if (error) {
        return (
            <div className="empty-state">
                <div className="empty-state-icon">🔒</div>
                <div className="empty-state-title">Kirish cheklangan</div>
                <div className="empty-state-text">{error}</div>
            </div>
        );
    }

    return (
        <>
            <header className="page-header">
                <div>
                    <h1 className="page-title">Restoranlarim</h1>
                    <p className="page-subtitle">{list.length} ta restoran</p>
                </div>
            </header>

            {list.length === 0 ? (
                <div className="empty-state">
                    <div className="empty-state-icon">🏪</div>
                    <div className="empty-state-title">Restoran yo'q</div>
                    <div className="empty-state-text">
                        Telegram bot orqali yangi restoran qo'shing, so'ng bu yerda menyuni boshqaring.
                    </div>
                </div>
            ) : (
                list.map((r, idx) => (
                    <article
                        key={r.id}
                        className="card card-restaurant"
                        style={{ animationDelay: `${idx * 0.08}s` }}
                    >
                        <h3>{r.name}</h3>
                        {r.description && <p className="desc">{r.description}</p>}
                        <div className="meta">
                            {r.address && <span className="meta-item">📍 {r.address}</span>}
                            {r.phone && <span className="meta-item">📞 {r.phone}</span>}
                            <span className="meta-item">📂 {r.categoryCount}</span>
                            <span className="meta-item">🍽 {r.itemCount}</span>
                        </div>
                        <div className="card-actions">
                            <button
                                className="btn btn-glass"
                                onClick={() => navigate(`/webapp/${userId}/restaurant/${r.id}`)}
                            >
                                Boshqarish
                            </button>
                        </div>
                    </article>
                ))
            )}
        </>
    );
}
