import { ReactNode } from 'react'

export default function Layout({ children }: { children: ReactNode }) {
  return (
    <div style={{ maxWidth: 480, margin: '0 auto' }}>
      {children}
    </div>
  )
}
