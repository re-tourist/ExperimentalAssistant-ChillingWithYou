// Simple in-memory TTL cache
const cache = new Map<string, { value: any; expiry: number }>()

export const dashboardCache = {
  get<T>(key: string): T | null {
    const item = cache.get(key)
    if (!item) return null
    if (Date.now() > item.expiry) {
      cache.delete(key)
      return null
    }
    return item.value as T
  },

  set(key: string, value: any, ttlSeconds: number = 30) {
    const expiry = Date.now() + ttlSeconds * 1000
    cache.set(key, { value, expiry })
  },

  clear() {
    cache.clear()
  }
}
