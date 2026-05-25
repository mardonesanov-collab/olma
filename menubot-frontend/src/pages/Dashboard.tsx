import { useEffect, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { useTelegram } from '../hooks/useTelegram'
import { api } from '../api/client'
import type { Restaurant } from '../types'

export default function Dashboard() {
  const { userId: paramUserId } = useParams()
  const { userId: tgUserId } = useTelegram()
  const navigate = useNavigate()
  const [restaurants, setRestaurants] = useState<Restaurant[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const userId = tgUserId || Number(paramUserId)

  useEffect(() => {
    if (!userId) return
    fetch(`/api/restaurants?userId=${userId}`)
      .then(res => {
        if (!res.ok) throw new Error('Ma\'lumot olishda xatolik')
        return res.json()
      })
      .then(setRestaurants)
      .catch(err => setError(err.message))
      .finally(() => setLoading(false))
  }, [userId])

  function copyLink(restId: number) {
    api.getMenuLink(restId, userId).then(res => {
      if (res.link) {
        navigator.clipboard?.writeText(res.link)
        alert('Link nusxalandi: ' + res.link)
      }
    })
  }

  if (!userId) {
    return <p className="card">Foydalanuvchi ID topilmadi. Iltimov /start ni bosing.</p>
  }

  if (loading) return <p className="card">Yuklanmoqda...</p>
  if (error) return <p className="card" style={{ color: '#e74c3c' }}>{error}</p>

  return (
    <div>
      <h1 className="header">Mening Restoranlarim</h1>

      {restaurants.length === 0 && (
        <div className="card" style={{ textAlign: 'center', padding: 32 }}>
          <p style={{ marginBottom: 8 }}>Hozircha restoran yo'q</p>
          <p className="subheader">Botda "Restoran qo'shish" tugmasini bosing</p>
        </div>
      )}

      {restaurants.map(r => (
        <div key={r.id} className="card" onClick={() => navigate(`/webapp/${userId}/restaurant/${r.id}`)} style={{ cursor: 'pointer' }}>
          <div style={{ fontSize: 18, fontWeight: 600, marginBottom: 4 }}>🍽 {r.name}</div>
          {r.description && <div style={{ fontSize: 14, marginBottom: 4, color: 'var(--tg-hint-color)' }}>{r.description}</div>}
          <div style={{ display: 'flex', gap: 8, marginTop: 8 }}>
            <button className="btn" onClick={e => { e.stopPropagation(); navigate(`/webapp/${userId}/restaurant/${r.id}`) }}>
              Boshqarish
            </button>
            <button className="btn btn-secondary" onClick={e => { e.stopPropagation(); copyLink(r.id) }}>
              Link olish
            </button>
          </div>
        </div>
      ))}
    </div>
  )
}
