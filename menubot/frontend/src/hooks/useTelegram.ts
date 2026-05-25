import { useEffect, useState } from 'react';

// Test rejimi uchun mock user
const MOCK_USER = {
    id: 123456789,
    first_name: 'Test',
    last_name: 'User',
    username: 'testuser',
    language_code: 'uz'
};

export function useTelegram() {
    const [webApp, setWebApp] = useState<any>(null);
    const [userId, setUserId] = useState<number | null>(null);
    const [ready, setReady] = useState(false);
    const [isMockMode, setIsMockMode] = useState(false);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        let finalUserId: number | null = null;
        let errorMessage: string | null = null;

        try {
            // 1. Telegram WebApp orqali
            const telegramGlobal = (window as any).Telegram;
            
            if (!telegramGlobal) {
                errorMessage = 'Telegram object topilmadi - brauzer Telegram WebApp qo\'llab-quvvatlamaydi';
            } else {
                const app = telegramGlobal.WebApp;
                
                if (!app) {
                    errorMessage = 'Telegram.WebApp mavjud emas';
                } else {
                    try {
                        app.ready();
                        app.expand();
                        setWebApp(app);

                        const tgUser = app.initDataUnsafe?.user;
                        if (tgUser?.id) {
                            finalUserId = Number(tgUser.id);
                        } else {
                            errorMessage = 'Telegram user ma\'lumotlari topilmadi';
                        }
                    } catch (e) {
                        errorMessage = `Telegram WebApp init xatosi: ${e instanceof Error ? e.message : String(e)}`;
                    }
                }
            }
        } catch (e) {
            errorMessage = `Umumiy xatolik: ${e instanceof Error ? e.message : String(e)}`;
        }

        // 2. Fallback: URL'dan olish (/webapp/:userId/...)
        if (!finalUserId) {
            try {
                const pathParts = window.location.pathname.split('/').filter(Boolean);
                // ["webapp", "USERID", ...]
                const webappIdx = pathParts.indexOf('webapp');
                if (webappIdx >= 0 && pathParts[webappIdx + 1]) {
                    const candidate = Number(pathParts[webappIdx + 1]);
                    if (!isNaN(candidate) && candidate > 0) {
                        finalUserId = candidate;
                        if (errorMessage) errorMessage += ' (URL dan foydalanildi)';
                    }
                }
            } catch (e) {
                if (errorMessage) errorMessage += `; URL parse xatosi: ${e instanceof Error ? e.message : String(e)}`;
            }
        }

        // 3. Test rejimi - mock user
        if (!finalUserId) {
            finalUserId = MOCK_USER.id;
            setIsMockMode(true);
            if (errorMessage) {
                errorMessage += ' -> Test rejimida mock user ishlatilmoqda';
            } else {
                errorMessage = 'Test rejimida mock user ishlatilmoqda';
            }
        }

        setError(errorMessage);
        setUserId(finalUserId);
        setReady(true);
    }, []);

    return { 
        webApp, 
        userId, 
        ready, 
        isMockMode, 
        error,
        mockUser: MOCK_USER 
    };
}
