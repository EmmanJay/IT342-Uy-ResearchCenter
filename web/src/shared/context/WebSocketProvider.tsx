import React, { createContext, useContext, useState, useEffect } from 'react';
import type { ReactNode } from 'react';
import { SessionManager } from '../auth/sessionManager';

interface WebSocketContextType {
  lastMessage: any;
  subscribe: (topic: string, callback: (msg: any) => void) => void;
  unsubscribe: (topic: string, callback: (msg: any) => void) => void;
}

const WebSocketContext = createContext<WebSocketContextType>({
  lastMessage: null,
  subscribe: () => {},
  unsubscribe: () => {}
});

export const WebSocketProvider: React.FC<{ children: ReactNode }> = ({ children }) => {
  const [lastMessage, setLastMessage] = useState<any>(null);

  useEffect(() => {
    const user = SessionManager.getUser();
    if (!user) return;
    
    // Polling simulation since WebSocket was removed in refactoring
    const interval = setInterval(() => {
      setLastMessage({ type: 'PING', timestamp: Date.now() });
    }, 15000);

    return () => clearInterval(interval);
  }, []);

  const subscribe = () => {};
  const unsubscribe = () => {};

  return (
    <WebSocketContext.Provider value={{ lastMessage, subscribe, unsubscribe }}>
      {children}
    </WebSocketContext.Provider>
  );
};

export const useWebSocket = () => useContext(WebSocketContext);
