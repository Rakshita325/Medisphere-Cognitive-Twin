/**
 * WebSocket service for real-time alert updates.
 * Uses @stomp/stompjs to connect to the Spring Boot STOMP broker.
 */
import { Client } from '@stomp/stompjs';

/**
 * Creates and activates a STOMP client that subscribes to /topic/alerts.
 *
 * @param {function} onAlert - Callback invoked with each alert message (parsed JSON).
 * @param {function} [onConnect] - Optional callback when connection is established.
 * @param {function} [onError] - Optional callback on connection error.
 * @returns {{ client: Client, deactivate: () => Promise<void> }}
 */
export function connectAlertWebSocket(onAlert, onConnect, onError) {
  // Determine WebSocket URL based on current page protocol/host.
  // In dev mode, Vite proxies /ws to the backend.
  const protocol = window.location.protocol === 'https:' ? 'wss' : 'ws';
  const wsUrl = `${protocol}://${window.location.host}/ws`;

  const client = new Client({
    brokerURL: wsUrl,

    // Reconnect after 5 seconds if connection drops
    reconnectDelay: 5000,

    // Heartbeat every 10 seconds
    heartbeatIncoming: 10000,
    heartbeatOutgoing: 10000,

    // Connection debug (disabled in production)
    // debug: (str) => console.log('[STOMP]', str),

    onConnect: () => {
      console.log('[WebSocket] Connected to /ws');

      // Subscribe to alert topic
      client.subscribe('/topic/alerts', (message) => {
        try {
          const alert = JSON.parse(message.body);
          onAlert(alert);
        } catch (err) {
          console.error('[WebSocket] Failed to parse alert message:', err);
        }
      });

      if (onConnect) onConnect();
    },

    onStompError: (frame) => {
      console.error('[WebSocket] STOMP error:', frame.headers?.message || 'Unknown error');
      if (onError) onError(frame);
    },

    onWebSocketError: (event) => {
      console.error('[WebSocket] Connection error:', event);
      if (onError) onError(event);
    },

    onDisconnect: () => {
      console.log('[WebSocket] Disconnected');
    },
  });

  client.activate();

  return {
    client,
    deactivate: async () => {
      if (client.active) {
        await client.deactivate();
        console.log('[WebSocket] Client deactivated');
      }
    },
  };
}
