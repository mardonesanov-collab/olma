import { useEffect } from 'react'

interface ToastProps {
    message: string
    type?: 'success' | 'error' | 'info'
    onClose: () => void
}

export default function Toast({ message, type = 'info', onClose }: ToastProps) {
    useEffect(() => {
        const timer = setTimeout(onClose, 3000)
        return () => clearTimeout(timer)
    }, [onClose])

    const icon = type === 'success' ? '✅' : type === 'error' ? '❌' : 'ℹ️'

    return (
        <div className={`toast toast-${type}`}>
            <span style={{ marginRight: 8 }}>{icon}</span>
            {message}
        </div>
    )
}