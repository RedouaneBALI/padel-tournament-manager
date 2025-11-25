import { NextApiRequest, NextApiResponse } from 'next'

// In-memory store of connected clients per game id
const clients: Map<string, Set<NextApiResponse>> = new Map()

function isAlive(res: NextApiResponse) {
  // res.writableEnded true signifie que la réponse est terminée
  // res.socket?.destroyed true signifie que le socket est fermé
  // @ts-ignore - some runtimes may not expose socket on the response type
  const socket = (res as any).socket
  return !res.writableEnded && !(socket && socket.destroyed)
}

function cleanClients(gameId: string) {
  const set = clients.get(gameId)
  if (!set) return
  for (const res of Array.from(set)) {
    if (!isAlive(res)) {
      set.delete(res)
    }
  }
  if (set.size === 0) clients.delete(gameId)
}

function sendCount(gameId: string) {
  cleanClients(gameId)
  const set = clients.get(gameId)
  const count = set ? set.size : 0
  const data = `data: ${count}\n\n`
  if (!set) return
  for (const res of Array.from(set)) {
    try {
      res.write(data)
    } catch (e) {
      // remove dead/broken connection
      try {
        set.delete(res)
      } catch (err) {
        // ignore
      }
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

  // Add this response to the clients set
  let set = clients.get(gameId)
  if (!set) {
    set = new Set()
    clients.set(gameId, set)
  }
  set.add(res)

  // Setup listeners to cleanup when the socket/response closes
  const cleanup = () => {
    clearInterval(heartbeat)
    const s = clients.get(gameId)
    if (s) {
      s.delete(res)
      if (s.size === 0) clients.delete(gameId)
      else sendCount(gameId)
    }
  }

  // Listen to different close events
  req.on('close', cleanup)
  res.on('close', cleanup)
  // @ts-ignore
  if (res.socket) {
    // @ts-ignore
    res.socket.on && res.socket.on('close', cleanup)
  }

  // Send initial count (clean first)
  cleanClients(gameId)
  const initial = `data: ${clients.get(gameId)?.size ?? 0}\n\n`
  try {
    res.write(initial)
  } catch (e) {
    // if write fails, cleanup and return
    cleanup()
    return
  }

  // Heartbeat to keep connection alive (every 15s)
  const heartbeat = setInterval(() => {
    try {
      res.write(': ping\n\n')
    } catch (e) {
      // on write error remove the connection
      cleanup()
    }
  }, 15000)

  // Broadcast updated count to all clients for this game
  sendCount(gameId)
}
