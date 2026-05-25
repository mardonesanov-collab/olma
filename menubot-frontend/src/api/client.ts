const BASE_URL = '/api';

async function request<T>(path: string, options?: RequestInit): Promise<T> {
  const res = await fetch(`${BASE_URL}${path}`, {
    headers: { 'Content-Type': 'application/json', ...options?.headers },
    ...options,
  });
  if (!res.ok) throw new Error(`HTTP ${res.status}`);
  return res.json();
}

export const api = {
  getCategories(restId: number) {
    return request<{ id: number; name: string; count: number }[]>(
      `/restaurant/${restId}/categories`
    );
  },

  getItems(catId: number) {
    return request<{
      id: number;
      name: string;
      price: number | null;
      description: string | null;
      photoPath: string | null;
    }[]>(`/category/${catId}/items`);
  },

  getMenuLink(restId: number, userId: number) {
    return request<{ success: boolean; link?: string; restaurantName?: string }>(
      `/restaurant/${restId}/link?userId=${userId}`
    );
  },

  async addCategory(userId: number, restaurantId: number, name: string) {
    const form = new URLSearchParams();
    form.append('userId', String(userId));
    form.append('restaurantId', String(restaurantId));
    form.append('name', name);
    return request<{ success: boolean; id?: number; name?: string; message?: string }>(
      '/category/add',
      { method: 'POST', body: form }
    );
  },

  async addItem(formData: FormData) {
    return request<{ success: boolean; id?: number; name?: string; photoPath?: string | null; message?: string }>(
      '/item/add',
      { method: 'POST', body: formData }
    );
  },
};
