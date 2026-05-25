export interface Restaurant {
  id: number;
  ownerId: number;
  name: string;
  address?: string;
  phone?: string;
  description?: string;
  photoPath?: string;
  uniqueLink: string;
  createdAt: string;
}

export interface MenuCategory {
  id: number;
  restaurantId: number;
  name: string;
  sortOrder: number;
  count?: number;
}

export interface MenuItem {
  id: number;
  restaurantId: number;
  categoryId: number;
  name: string;
  description?: string;
  price?: number;
  photoPath?: string;
  available: boolean;
  sortOrder: number;
}

export interface User {
  id: number;
  firstName: string;
  lastName?: string;
  username?: string;
  approved: boolean;
  admin: boolean;
}

export interface ApiResponse<T> {
  success: boolean;
  message?: string;
  data?: T;
}

export interface AddCategoryPayload {
  userId: number;
  restaurantId: number;
  name: string;
}

export interface AddItemPayload {
  userId: number;
  restaurantId: number;
  categoryId: number;
  name: string;
  description?: string;
  price?: number;
  photo?: File;
}
