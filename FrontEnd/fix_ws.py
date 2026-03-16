import re

file_path = '../FrontEnd/src/services/websocketService.ts'
with open(file_path, 'r') as f:
    content = f.read()

# Make sure wsService is exported only once naturally
instances = content.count('export const wsService')

if instances > 1:
    lines = content.split('\n')
    new_lines = []
    found = False
    for line in lines:
        if 'export const wsService' in line:
            if not found:
                new_lines.append(line)
                found = True
        else:
            new_lines.append(line)
    content = '\n'.join(new_lines)
    with open(file_path, 'w') as f:
        f.write(content)

