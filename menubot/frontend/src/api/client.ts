import type { PublicMenuData } from '../types';

const BASE_URL = '/api';

async function request<T>(path: string, options?: RequestInit): Promise<T> {
    const headers: Record<string, string> = { 
        ...(options?.headers as Record<string, string>),
        'ngrok-skip-browser-warning': 'true'  // Ngrok warning sahifasini o'tkazib yuborish
    };
    if (!(options?.body instanceof FormData)) {
        headers['Content-Type'] = 'application/json';
    }
    const res = await fetch(`${BASE_URL}${path}`, { ...options, headers });
    if (!res.ok) {
        const txt = await res.text().catch(() => '');
        throw new Error(`HTTP ${res.status}: ${txt || res.statusText}`);
    }
    return res.json();
}

async function upload(path: string, method: string, fd: FormData) {
    const res = await fetch(`${BASE_URL}${path}`, { 
        method, 
        body: fd,
        headers: { 'ngrok-skip-browser-warning': 'true' }
    });
    if (!res.ok) {
        const txt = await res.text().catch(() => '');
        throw new Error(`HTTP ${res.status}: ${txt || res.statusText}`);
    }
    return res.json();
}

export const api = {
    checkUser: (userId: number) => request<any>(`/user/${userId}/check`),

    getRestaurants: (userId: number) => request<any[]>(`/restaurants?userId=${userId}`),

    getCategories: (restId: number) => request<any[]>(`/restaurant/${restId}/categories`),
    addCategory: (userId: number, restId: number, name: string) =>
        request<any>(`/category/add?userId=${userId}&restaurantId=${restId}&name=${encodeURIComponent(name)}`,
            { method: 'POST' }),
    deleteCategory: (catId: number, userId: number) =>
        request<any>(`/category/${catId}?userId=${userId}`, { method: 'DELETE' }),

    getItems: (catId: number) => request<any[]>(`/category/${catId}/items`),
    addItem: (fd: FormData) => upload('/item/add', 'POST', fd),
    updateItem: (itemId: number, fd: FormData) => upload(`/item/${itemId}`, 'PUT', fd),
    deleteItem: (itemId: number, userId: number) =>
        request<any>(`/item/${itemId}?userId=${userId}`, { method: 'DELETE' }),

    getMenuLink: (restId: number, userId: number) =>
        request<any>(`/restaurant/${restId}/link?userId=${userId}`),

    getPublicMenu: (uniqueLink: string) =>
        request<PublicMenuData>(`/menu/${uniqueLink}`),

    // Legacy path bridge — use slug lookup when migrated
};
