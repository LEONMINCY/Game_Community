// 文件说明：utils/socket.js，WebSocket客户端工具，维护实时消息连接、重连和事件监听。
class WebSocketClient {
  constructor(url) {
    this.url = url;
    this.ws = null;
    this.listeners = new Map();
  }

  // 连接WebSocket
  connect() {
    return new Promise((resolve, reject) => {
      this.ws = new WebSocket(this.url);

      this.ws.onopen = () => {
        console.log('WebSocket连接成功');
        resolve();
      };

      this.ws.onerror = (error) => {
        console.error('WebSocket连接错误:', error);
        reject(error);
      };

      this.ws.onclose = () => {
        console.log('WebSocket连接关闭');
        this.reconnect();
      };

      this.ws.onmessage = (event) => {
        const data = JSON.parse(event.data);
        this.handleMessage(data);
      };
    });
  }

  // 重新连接
  reconnect() {
    console.log('尝试重新连接...');
    setTimeout(() => {
      this.connect();
    }, 3000);
  }

  // 发送消息
  sendMessage(type, data) {
    if (this.ws && this.ws.readyState === WebSocket.OPEN) {
      const message = JSON.stringify({
        type,
        data
      });
      this.ws.send(message);
    } else {
      console.error('WebSocket未连接');
    }
  }

  // 添加消息监听器
  on(type, callback) {
    if (!this.listeners.has(type)) {
      this.listeners.set(type, []);
    }
    this.listeners.get(type).push(callback);
  }

  // 移除消息监听器
  off(type, callback) {
    if (this.listeners.has(type)) {
      const callbacks = this.listeners.get(type);
      const index = callbacks.indexOf(callback);
      if (index !== -1) {
        callbacks.splice(index, 1);
      }
    }
  }

  // 处理接收到的消息
  handleMessage(message) {
    const { type, data } = message;
    if (this.listeners.has(type)) {
      this.listeners.get(type).forEach(callback => callback(data));
    }
  }

  // 关闭连接
  close() {
    if (this.ws) {
      this.ws.close();
    }
  }
}

// 创建WebSocket实例
const wsClient = new WebSocketClient('ws://your-websocket-server-url');

export default wsClient;
