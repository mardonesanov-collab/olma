import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import Layout from './components/Layout';
import Dashboard from './pages/Dashboard';
import RestaurantPage from './pages/RestaurantPage';
import PublicMenuPage from './pages/PublicMenuPage';
import ClientMenuPage from './pages/client/ClientMenuPage';
import VendorDashboard from './pages/vendor/VendorDashboard';
import DebugConsole from './components/DebugConsole';

export default function App() {
    return (
        <BrowserRouter>
            <Routes>
                {/* Mijoz — QR / startapp */}
                <Route path="/client/:restaurantId" element={<ClientMenuPage />} />
                <Route path="/menu/:uniqueLink" element={<PublicMenuPage />} />

                {/* Vendor admin */}
                <Route path="/webapp/:userId" element={<Layout><Dashboard /></Layout>} />
                <Route path="/webapp/:userId/restaurant/:restId" element={<Layout><RestaurantPage /></Layout>} />
                <Route path="/webapp/:userId/restaurant/:restId/orders" element={<Layout><VendorDashboard /></Layout>} />

                <Route path="/" element={<Navigate to="/webapp/0" replace />} />
                <Route path="*" element={<Navigate to="/webapp/0" replace />} />
            </Routes>
            {/* Debug Console - mobil qurilmalarda xatolarni ko'rish uchun */}
            <DebugConsole />
        </BrowserRouter>
    );
}
