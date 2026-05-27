import re
with open('src/main/resources/static/index.html', 'r', encoding='utf-8') as f:
    text = f.read()

st_app = text.find('function App()')
print('App start:', st_app)
st_form = text.find('function ProvisionForm({ onProvisioned }) {')
print('ProvisionForm start:', st_form)
st_tab = text.find('function ProvisionTab() {')
print('ProvisionTab start:', st_tab)
