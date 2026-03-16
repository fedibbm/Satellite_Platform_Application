import re

file_path = '../FrontEnd/src/services/websocketService.ts'
with open(file_path, 'r') as f:
    content = f.read()

# Add a generic subscribe method to the class
func = """
  public subscribeToTopic(topic: string, callback: (message: any) => void) {
    if (!this.client || !this.client.connected) {
      console.warn('Cannot subscribe, websocket not connected yet');
      // For a robust implementation, you might want to queue subscriptions
      return null;
    }
    
    return this.client.subscribe(topic, (message) => {
      try {
        const body = JSON.parse(message.body);
        callback(body);
      } catch (e) {
        callback(message.body);
      }
    });
  }
"""

if 'subscribeToTopic' not in content:
    idx = content.rfind('}')
    content = content[:idx] + func + '\n}'
    
    with open(file_path, 'w') as f:
        f.write(content)
