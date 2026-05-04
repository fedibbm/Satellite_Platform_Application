"use client"

import { useEffect } from 'react'
import { useRouter } from 'next/navigation'
import { useAuth } from '@/hooks/useAuth'

export default function AdminIndex() {
  const router = useRouter()
  const { user, loading } = useAuth()

  useEffect(() => {
    if (loading) return

    const roles: string[] = user?.roles || JSON.parse(localStorage.getItem('userRoles') || '[]')
    if (Array.isArray(roles) && roles.some(r => r.includes('ADMIN'))) {
      router.replace('/admin/dashboard')
    } else {
      router.replace('/auth/login')
    }
  }, [user, loading, router])

  return (
    <div className="min-h-screen flex items-center justify-center">
      <div className="text-gray-600">Redirecting to admin dashboard...</div>
    </div>
  )
}
