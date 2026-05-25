import { ReactNode } from 'react';

export default function Layout({ children }: { children: ReactNode }) {
    return (
        <div className="app-shell">
            <div className="container">
                {children}
            </div>
        </div>
    );
}
