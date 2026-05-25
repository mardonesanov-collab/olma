import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import Dashboard from './pages/Dashboard'
import RestaurantPage from './pages/RestaurantPage'
import Layout from './components/Layout'

export default function App() {
  return (
    <BrowserRouter>
      <Layout>
        <Routes>
          <Route path="/webapp/:userId" element={<Dashboard />} />
          <Route path="/webapp/:userId/restaurant/:restId" element={<RestaurantPage />} />
          <Route path="*" element={<Navigate to="/webapp/0" replace />} />
        </Routes>
      </Layout>
    </BrowserRouter>
  )
}
