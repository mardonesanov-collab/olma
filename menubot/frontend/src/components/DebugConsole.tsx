import { useState, useEffect } from 'react';

interface LogEntry {
    id: number;
    timestamp: string;
    type: 'log' | 'warn' | 'error' | 'info';
    message: string;
}

export default function DebugConsole() {
    const [logs, setLogs] = useState<LogEntry[]>([]);
    const [isOpen, setIsOpen] = useState(false);

    useEffect(() => {
        // Original console methodsini saqlab qolish
        const originalConsole = {
            log: console.log,
            warn: console.warn,
            error: console.error,
            info: console.info
        };

        // Console methodlarini intercept qilish
        const addLog = (type: 'log' | 'warn' | 'error' | 'info', args: any[]) => {
            const message = args.map(arg => {
                if (typeof arg === 'object') {
                    try {
                        return JSON.stringify(arg, null, 2);
                    } catch {
                        return String(arg);
                    }
                }
                return String(arg);
            }).join(' ');

            const entry: LogEntry = {
                id: Date.now() + Math.random(),
                timestamp: new Date().toLocaleTimeString('uz-UZ'),
                type,
                message
            };

            setLogs(prev => [...prev.slice(-49), entry]); // Oxirgi 50 ta logni saqlash
        };

        console.log = (...args: any[]) => {
            addLog('log', args);
            originalConsole.log(...args);
        };

        console.warn = (...args: any[]) => {
            addLog('warn', args);
            originalConsole.warn(...args);
        };

        console.error = (...args: any[]) => {
            addLog('error', args);
            originalConsole.error(...args);
        };

        console.info = (...args: any[]) => {
            addLog('info', args);
            originalConsole.info(...args);
        };

        // Xatoliklarni ushlash
        const handleError = (event: ErrorEvent) => {
            addLog('error', [`${event.message} (${event.filename}:${event.lineno}:${event.colno})`]);
        };

        const handlePromiseRejection = (event: PromiseRejectionEvent) => {
            addLog('error', [`Promise rejection: ${event.reason}`]);
        };

        window.addEventListener('error', handleError);
        window.addEventListener('unhandledrejection', handlePromiseRejection);

        // Dastlabki ma'lumot
        addLog('info', ['Debug Console ishga tushdi']);
        addLog('info', [`User Agent: ${navigator.userAgent}`]);
        addLog('info', [`URL: ${window.location.href}`]);

        return () => {
            // Tozalash
            console.log = originalConsole.log;
            console.warn = originalConsole.warn;
            console.error = originalConsole.error;
            console.info = originalConsole.info;
            window.removeEventListener('error', handleError);
            window.removeEventListener('unhandledrejection', handlePromiseRejection);
        };
    }, []);

    const getBgColor = (type: string) => {
        switch (type) {
            case 'error': return 'bg-red-900';
            case 'warn': return 'bg-yellow-900';
            case 'info': return 'bg-blue-900';
            default: return 'bg-gray-800';
        }
    };

    const getTextColor = (type: string) => {
        switch (type) {
            case 'error': return 'text-red-300';
            case 'warn': return 'text-yellow-300';
            case 'info': return 'text-blue-300';
            default: return 'text-gray-300';
        }
    };

    return (
        <div className="fixed bottom-0 left-0 right-0 z-50">
            {/* Toggle button */}
            <button
                onClick={() => setIsOpen(!isOpen)}
                className="w-full bg-gray-900 text-white px-4 py-2 text-sm font-mono flex justify-between items-center hover:bg-gray-800 transition-colors"
            >
                <span>🔧 Debug Console</span>
                <span className="flex items-center gap-2">
                    {logs.length > 0 && (
                        <span className="bg-red-500 text-white text-xs rounded-full px-2 py-0.5">
                            {logs.filter(l => l.type === 'error').length}
                        </span>
                    )}
                    <span>{isOpen ? '▲' : '▼'}</span>
                </span>
            </button>

            {/* Console panel */}
            {isOpen && (
                <div className="bg-gray-900 border-t border-gray-700 max-h-64 overflow-y-auto font-mono text-sm">
                    {/* Controls */}
                    <div className="flex gap-2 p-2 border-b border-gray-700">
                        <button
                            onClick={() => setLogs([])}
                            className="px-3 py-1 bg-red-600 text-white text-xs rounded hover:bg-red-700"
                        >
                            Tozalash
                        </button>
                        <button
                            onClick={() => {
                                const text = logs.map(l => `[${l.timestamp}] [${l.type.toUpperCase()}] ${l.message}`).join('\n');
                                navigator.clipboard.writeText(text);
                            }}
                            className="px-3 py-1 bg-blue-600 text-white text-xs rounded hover:bg-blue-700"
                        >
                            Nusxalash
                        </button>
                    </div>

                    {/* Logs */}
                    <div className="p-2">
                        {logs.length === 0 ? (
                            <div className="text-gray-500 text-center py-4">Loglar yo'q</div>
                        ) : (
                            logs.map((log) => (
                                <div
                                    key={log.id}
                                    className={`${getBgColor(log.type)} ${getTextColor(log.type)} px-2 py-1 mb-1 rounded font-mono text-xs whitespace-pre-wrap break-words`}
                                >
                                    <span className="text-gray-500">[{log.timestamp}]</span>
                                    <span className="ml-2 font-bold">[{log.type.toUpperCase()}]</span>
                                    <span className="ml-2">{log.message}</span>
                                </div>
                            ))
                        )}
                    </div>
                </div>
            )}
        </div>
    );
}