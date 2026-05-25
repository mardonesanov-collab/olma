import { useEffect, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { useTelegram } from '../hooks/useTelegram'
import { api } from '../api/client'
import AddItemForm from '../components/AddItemForm'
import type { MenuCategory, MenuItem, Restaurant } from '../types'

export default function RestaurantPage() {
  const { userId: paramUserId, restId } = useParams()
  const { userId: tgUserId } = useTelegram()
  const navigate = useNavigate()
  const userId = tgUserId || Number(paramUserId)

  const [restaurant, setRestaurant] = useState<Restaurant | null>(null)
  const [categories, setCategories] = useState<(MenuCategory & { items: MenuItem[] })[]>([])
  const [loading, setLoading] = useState(true)

  const [showAddCat, setShowAddCat] = useState(false)
  const [newCatName, setNewCatName] = useState('')
  const [addingCat, setAddingCat] = useState(false)
  const [activeCatId, setActiveCatId] = useState<number | null>(null)

  useEffect(() => {
    if (!restId) return
    loadData()
  }, [restId])

  async function loadData() {
    setLoading(true)
    try {
      const [r, cats] = await Promise.all([
        fetch(`/api/restaurants?userId=${userId}`).then(r => r.json()).then((list: Restaurant[]) => list.find(x => x.id === Number(restId))),
        api.getCategories(Number(restId)),
      ])
      setRestaurant(r || null)

      const catWithItems = await Promise.all(
        cats.map(async c => {
          const items = await api.getItems(c.id)
          return { ...c, items }
        })
      )
      setCategories(catWithItems)
    } catch (e) {
      console.error(e)
    } finally {
      setLoading(false)
    }
  }

  async function addCategory() {
    if (!newCatName.trim() || !restId) return
    setAddingCat(true)
    try {
      await api.addCategory(userId, Number(restId), newCatName.trim())
      setNewCatName('')
      setShowAddCat(false)
      loadData()
    } catch (e) {
      console.error(e)
    } finally {
      setAddingCat(false)
    }
  }

  function copyLink() {
    api.getMenuLink(Number(restId), userId).then(res => {
      if (res.link) {
        navigator.clipboard?.writeText(res.link)
        alert('Link nusxalandi: ' + res.link)
      }
    })
  }

  if (loading) return <p className="card">Yuklanmoqda...</p>
  if (!restaurant) return <p className="card">Restoran topilmadi</p>

  return (
    <div>
      <button className="btn btn-secondary" onClick={() => navigate(`/webapp/${userId}`)} style={{ marginBottom: 12 }}>
        ← Ortga
      </button>

      <h1 className="header">🍽 {restaurant.name}</h1>
      {restaurant.description && <p style={{ marginBottom: 12 }}>{restaurant.description}</p>}

      <button className="btn" onClick={copyLink}>🔗 Menyu linkini olish</button>

      <hr style={{ margin: '16px 0', border: 'none', borderTop: '1px solid var(--tg-secondary-bg-color)' }} />

      <h2 className="subheader">Kategoriyalar</h2>

      <button className="btn" onClick={() => setShowAddCat(!showAddCat)}>
        {showAddCat ? 'Bekor qilish' : '+ Kategoriya qo\'shish'}
      </button>

      {showAddCat && (
        <div className="card">
          <input className="input" placeholder="Kategoriya nomi" value={newCatName} onChange={e => setNewCatName(e.target.value)} autoFocus />
          <button className="btn" onClick={addCategory} disabled={addingCat || !newCatName.trim()}>
            {addingCat ? 'Saqlanmoqda...' : 'Saqlash'}
          </button>
        </div>
      )}

      {categories.length === 0 && (
        <div className="card" style={{ textAlign: 'center' }}>
          <p>Kategoriya yo'q. Yuqoridagi tugma orqali qo'shing.</p>
        </div>
      )}

      {categories.map(cat => (
        <div key={cat.id} className="card">
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 8 }}>
            <span style={{ fontWeight: 600, fontSize: 16 }}>📂 {cat.name}</span>
            <span className="badge">{cat.count}</span>
          </div>

          {cat.items.length === 0 && <p style={{ fontSize: 13, color: 'var(--tg-hint-color)' }}>Bu kategoriyada taom yo'q</p>}

          {cat.items.map(item => (
            <div key={item.id} className="item-row">
              <div>
                <div className="item-name">{item.name}</div>
                {item.description && <div style={{ fontSize: 13, color: 'var(--tg-hint-color)' }}>{item.description}</div>}
              </div>
              <div className="item-price">{item.price ? `${item.price.toLocaleString()} so'm` : ''}</div>
            </div>
          ))}

          {activeCatId === cat.id ? (
            <AddItemForm
              userId={userId}
              restaurantId={Number(restId)}
              categoryId={cat.id}
              onSuccess={() => { setActiveCatId(null); loadData() }}
              onCancel={() => setActiveCatId(null)}
            />
          ) : (
            <button className="btn btn-secondary" onClick={() => setActiveCatId(cat.id)} style={{ marginTop: 8 }}>
              + Taom qo'shish
            </button>
          )}
        </div>
      ))}
    </div>
  )
}
