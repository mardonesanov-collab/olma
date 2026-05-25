import { useState } from 'react'
import { api } from '../api/client'

interface Props {
  userId: number
  restaurantId: number
  categoryId: number
  onSuccess: () => void
  onCancel: () => void
}

export default function AddItemForm({ userId, restaurantId, categoryId, onSuccess, onCancel }: Props) {
  const [name, setName] = useState('')
  const [price, setPrice] = useState('')
  const [description, setDescription] = useState('')
  const [photo, setPhoto] = useState<File | null>(null)
  const [loading, setLoading] = useState(false)

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (!name.trim()) return

    setLoading(true)
    try {
      const fd = new FormData()
      fd.append('userId', String(userId))
      fd.append('restaurantId', String(restaurantId))
      fd.append('categoryId', String(categoryId))
      fd.append('name', name.trim())
      fd.append('description', description.trim())
      if (price) fd.append('price', String(Number(price.replace(/\s/g, ''))))
      if (photo) fd.append('photo', photo)

      await api.addItem(fd)
      onSuccess()
    } catch (e) {
      console.error(e)
    } finally {
      setLoading(false)
    }
  }

  return (
    <form onSubmit={handleSubmit}>
      <input className="input" placeholder="Taom nomi *" value={name} onChange={e => setName(e.target.value)} autoFocus />
      <input className="input" placeholder="Narxi (so'm)" type="number" value={price} onChange={e => setPrice(e.target.value)} />
      <input className="input" placeholder="Ta'rifi" value={description} onChange={e => setDescription(e.target.value)} />
      <input className="input" type="file" accept="image/*" onChange={e => setPhoto(e.target.files?.[0] || null)} />
      <button className="btn" type="submit" disabled={loading || !name.trim()}>
        {loading ? 'Saqlanmoqda...' : 'Saqlash'}
      </button>
      <button className="btn btn-secondary" type="button" onClick={onCancel}>Bekor qilish</button>
    </form>
  )
}
