import re

file_path = '../FrontEnd/src/services/websocketService.ts'
with open(file_path, 'r') as f:
    content = f.read()

getter = """
  public get isConnected(): boolean {
    return this.connected;
  }
"""

if 'public get isConnected' not in content:
    content = content.replace('class WebSocketService {', 'class WebSocketService {\n' + getter)
    with open(file_path, 'w') as f:
        f.write(content)
