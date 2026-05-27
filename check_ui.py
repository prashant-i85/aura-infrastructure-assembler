import re
with open('src/main/resources/static/index.html', 'r', encoding='utf-8') as f:
    text = f.read()

sub_f = re.search(r'async function submit\(e\)(.*?)\n      \}', text, flags=re.DOTALL)
if sub_f: print('SUBMIT:\n', sub_f.group(0)[:800])

prov_d = re.search(r'function ProvisionDashboard\(\)(.*?)return \(', text, flags=re.DOTALL)
if prov_d: print('PROV DASH:\n', prov_d.group(0)[:300])

rcol = re.search(r'<div className="col-right">(.*?)<!-- Extra', text, flags=re.DOTALL)
if rcol: print('\nRIGHT COL:\n', rcol.group(0)[:500])

