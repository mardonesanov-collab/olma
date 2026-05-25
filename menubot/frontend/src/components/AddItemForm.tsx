import { useState } from 'react';
import { api } from '../api/client';

interface ItemData {
    id: number;
    name: string;
    price?: number | null;
    description?: string | null;
    photoPath?: string | null;
    available?: boolean;
}

interface Props {
    userId: number;
    restaurantId: number;
    categoryId: number;
    item?: ItemData;
    onSuccess: () => void;
    onCancel: () => void;
    onError?: (msg: string) => void;
}

export default function AddItemForm({
    userId,
    restaurantId,
    categoryId,
    item,
    onSuccess,
    onCancel,
    onError,
}: Props) {
    const [name, setName] = useState(item?.name ?? '');
    const [price, setPrice] = useState(
        item?.price ? Number(item.price).toLocaleString('uz-UZ').replace(/,/g, ' ') : ''
    );
    const [description, setDescription] = useState(item?.description ?? '');
    const [available, setAvailable] = useState(item?.available !== false);
    const [photo, setPhoto] = useState<File | null>(null);
    const [photoPreview, setPhotoPreview] = useState<string | null>(item?.photoPath ?? null);
    const [loading, setLoading] = useState(false);

    function handleFileChange(e: React.ChangeEvent<HTMLInputElement>) {
        const file = e.target.files?.[0];
        if (file) {
            setPhoto(file);
            const reader = new FileReader();
            reader.onload = (ev) => setPhotoPreview(ev.target?.result as string);
            reader.readAsDataURL(file);
        }
    }

    function removePhoto() {
        setPhoto(null);
        setPhotoPreview(null);
    }

    function formatPrice(value: string) {
        const num = value.replace(/\D/g, '');
        return num.replace(/\B(?=(\d{3})+(?!\d))/g, ' ');
    }

    async function handleSubmit(e: React.FormEvent) {
        e.preventDefault();
        if (!name.trim()) return;

        setLoading(true);
        try {
            const fd = new FormData();
            fd.append('userId', String(userId));
            fd.append('name', name.trim());
            fd.append('description', description.trim());
            if (price) fd.append('price', String(Number(price.replace(/\s/g, ''))));
            if (photo) fd.append('photo', photo);

            if (item) {
                fd.append('available', String(available));
                await api.updateItem(item.id, fd);
            } else {
                fd.append('restaurantId', String(restaurantId));
                fd.append('categoryId', String(categoryId));
                await api.addItem(fd);
            }
            onSuccess();
        } catch {
            onError?.("Saqlashda xatolik yuz berdi");
        } finally {
            setLoading(false);
        }
    }

    return (
        <form onSubmit={handleSubmit} className="form" style={{ marginTop: 16 }}>
            <div className="input-group">
                <span className="input-icon">🍽</span>
                <input
                    className="input input-with-icon"
                    placeholder="Taom nomi *"
                    value={name}
                    onChange={e => setName(e.target.value)}
                    autoFocus
                />
            </div>

            <div className="input-group">
                <span className="input-icon">💰</span>
                <input
                    className="input input-with-icon"
                    placeholder="Narxi (so'm)"
                    inputMode="numeric"
                    value={price}
                    onChange={e => setPrice(formatPrice(e.target.value))}
                />
            </div>

            <div className="input-group">
                <span className="input-icon">📝</span>
                <input
                    className="input input-with-icon"
                    placeholder="Ta'rifi (ixtiyoriy)"
                    value={description}
                    onChange={e => setDescription(e.target.value)}
                />
            </div>

            {item && (
                <div className="form-toggle">
                    <label>Mijozlarga ko'rinadi</label>
                    <button
                        type="button"
                        className={`toggle${available ? ' on' : ''}`}
                        onClick={() => setAvailable(!available)}
                        aria-pressed={available}
                    />
                </div>
            )}

            <div className="file-upload">
                <input type="file" accept="image/*" onChange={handleFileChange} />
                <div className="file-upload-label">
                    <div style={{ fontSize: 28 }}>📸</div>
                    <div className="file-upload-text">
                        {photo ? photo.name : 'Rasm yuklash uchun bosing'}
                    </div>
                </div>
            </div>

            {photoPreview && (
                <div className="file-preview">
                    <img src={photoPreview} alt="preview" />
                    <button type="button" className="file-preview-remove" onClick={removePhoto}>
                        ✕
                    </button>
                </div>
            )}

            <div className="form-row" style={{ marginTop: 8 }}>
                <button className="btn btn-secondary" type="button" onClick={onCancel}>
                    Bekor qilish
                </button>
                <button className="btn btn-primary" type="submit" disabled={loading || !name.trim()}>
                    {loading ? (
                        <>
                            <span className="spinner" />
                            Saqlanmoqda...
                        </>
                    ) : (
                        item ? 'Yangilash' : 'Saqlash'
                    )}
                </button>
            </div>
        </form>
    );
}
