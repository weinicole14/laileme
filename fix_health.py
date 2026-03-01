import json
d2 = json.load(open('/opt/laileme-api/sync_data/user_2.json'))
h2 = d2.get('healthData', {})
clean = {}
for k, v in h2.items():
    if isinstance(v, dict):
        clean[k] = v
d2['healthData'] = clean
json.dump(d2, open('/opt/laileme-api/sync_data/user_2.json', 'w'), indent=2)
print('female fixed:', json.dumps(clean))
d3 = json.load(open('/opt/laileme-api/sync_data/user_3.json'))
d3['healthData'] = {}
json.dump(d3, open('/opt/laileme-api/sync_data/user_3.json', 'w'), indent=2)
print('male cleared')
