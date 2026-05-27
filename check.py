import re
with open('src/main/resources/static/index.html', 'r', encoding='utf-8') as f:
    text = f.read()

submit_match = re.search(r'async function submit\(e\)(.*?)\n      \}', text, flags=re.DOTALL)
if submit_match:
    print('SUBMIT FUNC:', submit_match.group(0)[:500])

prov_dash = re.search(r'function ProvisionDashboard\(\)(.*?)return \(', text, flags=re.DOTALL)
if prov_dash:
    print('\nPROVISION:', prov_dash.group(0)[:100])

status_tracker = re.search(r'function StatusTracker\(.*?\)(.*?)\n    \}', text, flags=re.DOTALL)
if status_tracker:
    print('\nSTATUS TRACKER:', status_tracker.group(0)[:300])

