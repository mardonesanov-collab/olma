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
    count?: number; // Ixtiyoriy, chunki API'dan har doim ham kelmasligi mumkin
}

export interface MenuItem {
    id: number;
    restaurantId: number;
    categoryId: number;
    name: string;
    description?: string;
    price?: number | null;
    photoPath?: string | null;
    available: boolean;
    sortOrder: number;
}

export interface PublicRestaurant {
    id: number;
    name: string;
    address?: string;
    phone?: string;
    description?: string;
    photoPath?: string | null;
}

export interface PublicMenuItem {
    id: number;
    name: string;
    description?: string;
    price?: number | null;
    photoPath?: string | null;
}

export interface PublicCategory {
    id: number;
    name: string;
    items: PublicMenuItem[];
}

export interface PublicMenuData {
    restaurant: PublicRestaurant;
    categories: PublicCategory[];
}