import React from 'react'
import ReactDOM from 'react-dom/client'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'

import App from './App'
import { AuthProvider } from '@/context/AuthContext'
import './index.css'

const queryClient = new QueryClient()

const bootstrap = async () => {
  // Temporarily disable MSW mocks to test real backend
  // if (import.meta.env.DEV) {
  //   const { worker } = await import('./mocks/browser')
  //   await worker.start()
  // }

  ReactDOM.createRoot(document.getElementById('root')!).render(
    <React.StrictMode>
      <QueryClientProvider client={queryClient}>
        <AuthProvider>
          <App />
        </AuthProvider>
      </QueryClientProvider>
    </React.StrictMode>,
  )
}

void bootstrap()
