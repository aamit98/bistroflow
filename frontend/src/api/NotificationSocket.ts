import { Client, type IMessage } from '@stomp/stompjs'
import SockJS from 'sockjs-client'

let client: Client | null = null
const pendingBranchSubs: Array<{ branchId: number; cb: (msg: any) => void }> = []

export type NotificationMessage = {
  type: string
  [k: string]: any
}

export function connectSocket(token: string | null, onMessage: (msg: NotificationMessage) => void) {
  if (client) return

  client = new Client({
    // use SockJS factory — brokerURL not used when webSocketFactory is set
    webSocketFactory: () => new SockJS('/ws'),
    connectHeaders: token ? { Authorization: `Bearer ${token}` } : {},
    debug: () => {},
    reconnectDelay: 5000,
    // heartbeats disabled for simplicity; can be enabled later
  })

  client.onConnect = () => {
    console.info('[STOMP] connected')
    // subscribe to personal queue for notifications
    client!.subscribe('/user/queue/notifications', (message: IMessage) => {
      try {
        const body = JSON.parse(message.body)
        onMessage(body)
      } catch (e) {
        // ignore invalid payloads
      }
    })

    // process any pending branch subscriptions
    while (pendingBranchSubs.length > 0) {
      const sub = pendingBranchSubs.shift()!
      client!.subscribe(`/topic/hr/branch/${sub.branchId}`, (message: IMessage) => {
        try {
          const body = JSON.parse(message.body)
          sub.cb(body)
        } catch (e) {
          // ignore
        }
      })
    }
  }

  client.onStompError = (frame) => {
    console.error('Broker error', frame)
  }

  client.activate()
}

export function disconnectSocket() {
  if (!client) return
  client.deactivate()
  client = null
}

export function subscribeToBranch(branchId: number, onMessage: (msg: NotificationMessage) => void) {
  if (!client) {
    // queue until a client is created
    pendingBranchSubs.push({ branchId, cb: onMessage })
    return
  }

  // if client exists but not connected yet, queue it
  // `connected` is true once onConnect has fired
  // use `client.connected` if available, otherwise rely on pending queue
  // @ts-ignore
  if ((client as any).connected !== true) {
    pendingBranchSubs.push({ branchId, cb: onMessage })
    return
  }

  client.subscribe(`/topic/hr/branch/${branchId}`, (message: IMessage) => {
    try {
      const body = JSON.parse(message.body)
      onMessage(body)
    } catch (e) {
      // ignore
    }
  })
}
