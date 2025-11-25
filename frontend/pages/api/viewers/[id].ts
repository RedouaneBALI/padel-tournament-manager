import { NextApiRequest, NextApiResponse } from 'next'

// In-memory store of connected clients per game id
// Map<gameId, Map<clientId, NextApiResponse>>
const clients: Map<string, Map<string, NextApiResponse>> = new Map()

function isAlive(res: NextApiResponse) {
  const socket = (res as any).socket
  return !res.writableEnded && !(socket && socket.destroyed)
}

function cleanClients(gameId: string) {
  const map = clients.get(gameId)
  if (!map) return
  for (const [clientId, res] of Array.from(map.entries())) {
    if (!isAlive(res)) map.delete(clientId)
  }
  if (map.size === 0) clients.delete(gameId)
}

function sendCount(gameId: string) {
  cleanClients(gameId)
  const map = clients.get(gameId)
  const count = map ? map.size : 0
  const data = `data: ${count}\n\n`
  if (!map) return
  for (const res of Array.from(map.values())) {
    try {
      res.write(data)
    } catch (e) {
      // ignore
    }
  }
}

export default function handler(req: NextApiRequest, res: NextApiResponse) {
  const { id } = req.query
  if (!id || Array.isArray(id)) {
    res.status(400).end('missing id')
    return
  }
  const gameId = id as string

  // Set SSE headers
  res.setHeader('Content-Type', 'text/event-stream')
  res.setHeader('Cache-Control', 'no-cache, no-transform')
  res.setHeader('Connection', 'keep-alive')
  res.flushHeaders?.()

  // Read clientId from query param (optional)
  const rawClientId = req.query.clientId
  const clientId = typeof rawClientId === 'string' && rawClientId ? rawClientId : Math.random().toString(36).slice(2)

  // Add to map
  let map = clients.get(gameId)
  if (!map) {
    map = new Map()
    clients.set(gameId, map)
  }

  // If same client reconnects, replace the existing response
  const existing = map.get(clientId)
  if (existing && existing !== res) {
    try {
      existing.end()
    } catch (e) {
      // ignore
    }
    map.delete(clientId)
  }

  map.set(clientId, res)

  const cleanup = () => {
    clearInterval(heartbeat)
    const m = clients.get(gameId)
    if (m) {
      m.delete(clientId)
      if (m.size === 0) clients.delete(gameId)
      else sendCount(gameId)
    }
  }

  req.on('close', cleanup)
  res.on('close', cleanup)
  if ((res as any).socket) {
    ;(res as any).socket.on && (res as any).socket.on('close', cleanup)
  }

  // initial send
  cleanClients(gameId)
  try {
    res.write(`data: ${clients.get(gameId)?.size ?? 0}\n\n`)
  } catch (e) {
    cleanup()
    return
  }

  const heartbeat = setInterval(() => {
    try {
      res.write(': ping\n\n')
    } catch (e) {
      cleanup()
    }
  }, 15000)

  sendCount(gameId)
}
