import re
with open('src/main/resources/static/index.html', 'r', encoding='utf-8') as f:
    text = f.read()

b = re.search(r'<button type="submit".*?</button>', text, flags=re.DOTALL)
if b: print(b.group(0))
