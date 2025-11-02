import json
import sys

data = json.load(sys.stdin)
plugins = data.get('plugins', [])

totals = {}
roles = {}
deps_count = 0

for p in plugins:
    cat = p.get('category', 'Unknown')
    totals[cat] = totals.get(cat, 0) + 1
    
    s = p.get('semantics', {})
    r = s.get('role', 'NO_ROLE')
    roles[r] = roles.get(r, 0) + 1
    
    if p.get('dependencies'):
        deps_count += 1

print(f'Total: {len(plugins)} plugins')
print(f'\nBy Category:')
for k, v in sorted(totals.items()):
    print(f'  {k}: {v}')
print(f'\nBy Role:')
for k, v in sorted(roles.items()):
    print(f'  {k}: {v}')
print(f'\nPlugins with dependencies: {deps_count}')

