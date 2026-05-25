import { useTelegram } from './useTelegram';

const API = '/api/v1';

async function request<T>(path: string, options?: RequestInit): Promise<T> {
    const tg = (window as any).Telegram?.WebApp;
    const initData = tg?.initData || '';

    const headers: Record<string, string> = {
        ...(options?.headers as Record<string, string>),
        'X-Telegram-Init-Data': initData,
    };

    if (!(options?.body instanceof FormData)) {
        headers['Content-Type'] = 'application/json';
    }

    const res = await fetch(`${API}${path}`, { ...options, headers });
    if (!res.ok) {
        const err = await res.json().catch(() => ({}));
        throw new Error(err.error || `HTTP ${res.status}`);
    }
    if (res.status === 204) return undefined as T;
    return res.json();
}

export function useApi() {
    const { webApp } = useTelegram();

    return {
        haptic: (type: 'light' | 'medium' | 'heavy' | 'success' = 'light') => {
            webApp?.HapticFeedback?.impactOccurred?.(type);
        },
        public: {
            restaurant: (id: number) => request<any>(`/public/restaurants/${id}`),
            menu: (id: number, q?: string) =>
                request<any>(`/public/restaurants/${id}/menu${q ? `?q=${encodeURIComponent(q)}` : ''}`),
            parseStartApp: (startapp: string) =>
                request<{ restaurantId: number; tableNumber: string }>(`/public/parse-startapp?startapp=${encodeURIComponent(startapp)}`),
        },
        vendor: {
            restaurants: () => request<any[]>('/vendor/restaurants'),
            register: (name: string, description?: string) => {
                const fd = new FormData();
                fd.append('name', name);
                if (description) fd.append('description', description);
                return request<any>('/vendor/restaurants', { method: 'POST', body: fd });
            },
            orders: (restaurantId: number) => request<any[]>(`/vendor/restaurants/${restaurantId}/orders`),
            analytics: (restaurantId: number) => request<any>(`/vendor/restaurants/${restaurantId}/analytics`),
            updateOrderStatus: (restaurantId: number, orderId: number, status: string) =>
                request<any>(`/vendor/restaurants/${restaurantId}/orders/${orderId}/status?status=${status}`, { method: 'PATCH' }),
            addCategory: (restaurantId: number, name: string) => {
                const fd = new FormData();
                fd.append('name', name);
                return request<any>(`/vendor/restaurants/${restaurantId}/categories`, { method: 'POST', body: fd });
            },
            addProduct: (restaurantId: number, data: FormData) =>
                request<any>(`/vendor/restaurants/${restaurantId}/products`, { method: 'POST', body: data }),
            toggleProduct: (restaurantId: number, productId: number, available: boolean) => {
                const fd = new FormData();
                fd.append('available', String(available));
                return request<any>(`/vendor/restaurants/${restaurantId}/products/${productId}`, { method: 'PUT', body: fd });
            },
            generateQr: (restaurantId: number, tableNumber: string) =>
                request<{ qrUrl: string }>(`/vendor/restaurants/${restaurantId}/qr?tableNumber=${encodeURIComponent(tableNumber)}`, { method: 'POST' }),
        },
        client: {
            placeOrder: (restaurantId: number, tableNumber: string | undefined, items: { productId: number; quantity: number }[]) =>
                request<any>(`/client/restaurants/${restaurantId}/orders?${tableNumber ? `tableNumber=${encodeURIComponent(tableNumber)}` : ''}`, {
                    method: 'POST',
                    body: JSON.stringify(items),
                }),
            callWaiter: (restaurantId: number, tableNumber: string) =>
                request<any>(`/client/restaurants/${restaurantId}/call-waiter?tableNumber=${encodeURIComponent(tableNumber)}`, { method: 'POST' }),
            review: (restaurantId: number, orderId: number, rating: number, comment?: string) => {
                const fd = new FormData();
                fd.append('orderId', String(orderId));
                fd.append('rating', String(rating));
                if (comment) fd.append('comment', comment);
                return request<any>(`/client/restaurants/${restaurantId}/reviews`, { method: 'POST', body: fd });
            },
        },
    };
}
