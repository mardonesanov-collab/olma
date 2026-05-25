import { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { useApi } from '../../hooks/useApi';
import Toast from '../../components/Toast';

export default function VendorDashboard() {
    const { restId } = useParams();
    const api = useApi();
    const restaurantId = Number(restId);

    const [orders, setOrders] = useState<any[]>([]);
    const [analytics, setAnalytics] = useState<any>(null);
    const [newOrderSound] = useState(() => typeof Audio !== 'undefined' ? new Audio('data:audio/wav;base64,UklGRnoGAABXQVZFZm10IBAAAAABAAEAQB8AAEAfAAABAAgAZGF0YQoGAACBhYqFbF1fdJivrJBhNjVgodDbq2EcBj+a2/LDciUFLIHO8tiJNwgZaLvt559NEAxQp+PwtmMcBjiR1/LMeSwFJHfH8N2QQAoUXrTp66hVFApGn+DyvmwhBSuBzvLZiTYIGWi77+efTRAMUKfj8LZjHAY4kdfyzHksBSR3x/DdkEAKFF606euoVRQKRp/g8r5sIQUrgc7y2Yk2CBlou+/nn00QDFCn4/C2YxwGOJHX8sx5LAUkd8fw3ZBAC') : null);
    const [toast, setToast] = useState<{ msg: string; type: 'success' | 'error' } | null>(null);

    const load = async () => {
        try {
            const [o, a] = await Promise.all([
                api.vendor.orders(restaurantId),
                api.vendor.analytics(restaurantId),
            ]);
            if (orders.length > 0 && o.length > orders.length) {
                newOrderSound?.play().catch(() => {});
            }
            setOrders(o);
            setAnalytics(a);
        } catch (e: any) {
            setToast({ msg: e.message, type: 'error' });
        }
    };

    useEffect(() => {
        load();
        const t = setInterval(load, 15000);
        return () => clearInterval(t);
    }, [restaurantId]);

    async function setStatus(orderId: number, status: string) {
        try {
            await api.vendor.updateOrderStatus(restaurantId, orderId, status);
            load();
        } catch (e: any) {
            setToast({ msg: e.message, type: 'error' });
        }
    }

    return (
        <>
            <header className="page-header">
                <div>
                    <h1 className="page-title">Live Orders</h1>
                    <p className="page-subtitle">Real-time boshqaruv paneli</p>
                </div>
            </header>

            {analytics && (
                <div className="analytics-grid">
                    <div className="card stat-card">
                        <span className="stat-label">Bugun</span>
                        <span className="stat-value">{Number(analytics.todayRevenue).toLocaleString()} so'm</span>
                        <span className="stat-meta">{analytics.todayOrders} buyurtma</span>
                    </div>
                    <div className="card stat-card">
                        <span className="stat-label">Hafta</span>
                        <span className="stat-value">{Number(analytics.weekRevenue).toLocaleString()} so'm</span>
                    </div>
                    <div className="card stat-card">
                        <span className="stat-label">Oy</span>
                        <span className="stat-value">{Number(analytics.monthRevenue).toLocaleString()} so'm</span>
                    </div>
                </div>
            )}

            {orders.length === 0 ? (
                <div className="empty-state">
                    <div className="empty-state-title">Buyurtmalar yo'q</div>
                </div>
            ) : (
                orders.map(order => (
                    <div key={order.id} className="card order-card">
                        <div className="order-card-head">
                            <strong>#{order.id}</strong>
                            <span className={`badge badge-${order.status?.toLowerCase()}`}>{order.status}</span>
                        </div>
                        <p>Stol: {order.tableNumber || '—'} · {Number(order.finalPrice).toLocaleString()} so'm</p>
                        <div className="form-row">
                            {order.status === 'NEW' && (
                                <button className="btn btn-primary btn-sm" onClick={() => setStatus(order.id, 'PREPARING')}>
                                    Tayyorlanmoqda
                                </button>
                            )}
                            {order.status === 'PREPARING' && (
                                <button className="btn btn-primary btn-sm" onClick={() => setStatus(order.id, 'DELIVERED')}>
                                    Yetkazildi
                                </button>
                            )}
                            {order.status !== 'CANCELLED' && order.status !== 'DELIVERED' && (
                                <button className="btn btn-secondary btn-sm" onClick={() => setStatus(order.id, 'CANCELLED')}>
                                    Bekor
                                </button>
                            )}
                        </div>
                    </div>
                ))
            )}

            {toast && <Toast message={toast.msg} type={toast.type} onClose={() => setToast(null)} />}
        </>
    );
}
