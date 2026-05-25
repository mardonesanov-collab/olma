import { useEffect, useState } from 'react';

export function useTelegram() {
  const [webApp, setWebApp] = useState<Window['Telegram']['WebApp'] | null>(null);
  const [userId, setUserId] = useState<number | null>(null);

  useEffect(() => {
    const app = window.Telegram?.WebApp;
    if (app) {
      app.ready();
      app.expand();
      setWebApp(app);

      const tgUser = app.initDataUnsafe?.user;
      if (tgUser) {
        setUserId(tgUser.id);
      }
    }

    const params = new URLSearchParams(window.location.pathname.split('/'));
    const pathParts = window.location.pathname.split('/');
    const idFromPath = pathParts[2] ? Number(pathParts[2]) : null;

    if (!userId && idFromPath) {
      setUserId(idFromPath);
    }
  }, []);

  return { webApp, userId };
}
