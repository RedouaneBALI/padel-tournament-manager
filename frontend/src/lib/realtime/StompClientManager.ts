import { Client, IMessage, StompHeaders } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

type Listener = (message: IMessage) => void;

type SubscriptionEntry = {
  destination: string;
  listener: Listener;
  id?: string;
};

type AuthTokenProvider = () => Promise<string | null>;

type StompManagerConfig = {
  baseUrl?: string;
  authTokenProvider?: AuthTokenProvider;
  heartbeatIncoming?: number;
  heartbeatOutgoing?: number;
  reconnectDelay?: number;
};

/**
 * Minimal STOMP manager to share a single connection across listeners.
 */
export class StompClientManager {
  private client: Client | null = null;
  private readonly listeners = new Map<string, SubscriptionEntry>();
  private readonly config: Required<Omit<StompManagerConfig, 'authTokenProvider'>> & Pick<StompManagerConfig, 'authTokenProvider'>;
  private connecting = false;

  constructor(config: StompManagerConfig = {}) {
    const baseUrl = config.baseUrl ?? (process.env.NEXT_PUBLIC_API_BASE_URL ?? 'http://localhost:8080');
    // SockJS expects http/https URLs, not ws/wss
    const wsEndpoint = baseUrl + '/ws';
    this.config = {
      baseUrl: wsEndpoint,
      heartbeatIncoming: config.heartbeatIncoming ?? 10000,
      heartbeatOutgoing: config.heartbeatOutgoing ?? 10000,
      reconnectDelay: config.reconnectDelay ?? 5000,
      authTokenProvider: config.authTokenProvider,
    } as any;
  }

  async subscribe(destination: string, listener: Listener) {
    this.listeners.set(destination + listener.toString(), { destination, listener });
    await this.ensureConnected();
    this.installSubscription(destination, listener);
    return () => this.unsubscribe(destination, listener);
  }

  private async ensureConnected() {
    if (this.client?.connected || this.connecting) return;
    this.connecting = true;

    const client = new Client({
      // Don't set brokerURL when using webSocketFactory with SockJS
      reconnectDelay: this.config.reconnectDelay,
      heartbeatIncoming: this.config.heartbeatIncoming,
      heartbeatOutgoing: this.config.heartbeatOutgoing,
      webSocketFactory: () => new SockJS(this.config.baseUrl, undefined, { transports: ['websocket', 'xhr-streaming', 'xhr-polling'] }),
      connectHeaders: await this.buildAuthHeaders(),
      onConnect: () => {
        this.connecting = false;
        // Reinstall all subscriptions (important for reconnections)
        this.listeners.forEach(({ destination, listener }) => this.installSubscription(destination, listener));
      },
      onDisconnect: () => {
        this.connecting = false;
      },
      onWebSocketClose: () => {
        this.connecting = false;
      },
      onStompError: (frame: any) => {
        console.error('[STOMP] Error', frame.headers['message'], frame.body);
        this.connecting = false;
      },
    });

    client.activate();
    this.client = client;
  }

  private installSubscription(destination: string, listener: Listener) {
    if (!this.client?.connected) return;
    const key = destination + listener.toString();
    const existing = this.listeners.get(key);
    if (existing?.id) return; // Already subscribed
    const sub = this.client.subscribe(destination, listener);
    this.listeners.set(key, { destination, listener, id: sub.id });
  }

  private async buildAuthHeaders(): Promise<StompHeaders> {
    const token = await this.config.authTokenProvider?.();
    if (!token) return {};
    return { Authorization: `Bearer ${token}` };
  }

  private unsubscribe(destination: string, listener: Listener) {
    const key = destination + listener.toString();
    const entry = this.listeners.get(key);
    if (!entry) return;

    if (entry.id && this.client?.connected) {
      this.client.unsubscribe(entry.id);
    }
    this.listeners.delete(key);

    // Only deactivate if no more listeners AND client exists
    // Don't deactivate immediately to allow for reconnections
    if (this.listeners.size === 0 && this.client) {
      // Delay deactivation to avoid rapid connect/disconnect cycles
      setTimeout(() => {
        if (this.listeners.size === 0 && this.client) {
          this.client.deactivate();
          this.client = null;
          this.connecting = false;
        }
      }, 1000);
    }
  }
}
