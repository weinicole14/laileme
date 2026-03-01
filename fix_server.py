import sys

with open('/opt/laileme-api/server.js', 'r') as f:
    c = f.read()

fixes = 0

# 1. dedup null protection
old1 = 'function dedup(arr, key) {\n            const map = {};\n            arr.forEach(function(item) { map[item[key]] = item; });\n            return Object.values(map);\n        }'
new1 = 'function dedup(arr, key) { if (!Array.isArray(arr) || arr.length === 0) return arr || []; var map = {}; arr.forEach(function(item) { if (item && item[key] !== undefined) map[item[key]] = item; }); return Object.values(map); }'
if old1 in c:
    c = c.replace(old1, new1, 1)
    fixes += 1
    print('1. dedup null protection: FIXED')
else:
    print('1. dedup: pattern not found')
    if 'function dedup' in c:
        print('   dedup exists but pattern differs')

# 2. Unify admin key
count_old = c.count('laileme_admin_2024')
if count_old > 0:
    c = c.replace('laileme_admin_2024', 'laileme_admin_2025')
    fixes += 1
    print('2. admin key unified: %d occurrences FIXED' % count_old)
else:
    print('2. admin key: already unified')

# 3. Body size limit
old3 = 'app.use(express.json());'
new3 = "app.use(express.json({ limit: '2mb' }));"
if old3 in c:
    c = c.replace(old3, new3, 1)
    fixes += 1
    print('3. body size limit: FIXED')
else:
    print('3. body size limit: already set')

# 4. Login rate limit
old4 = "// \xe2\x94\x80\xe2\x94\x80 \xe7\x99\xbb\xe5\xbd\x95 \xe2\x94\x80\xe2\x94\x80\napp.post('/api/auth/login', (req, res) => {\n    try {"
new4 = """// ── 简易登录限流 ──
const loginAttempts = {};
function checkLoginRate(ip) {
    const now = Date.now();
    if (!loginAttempts[ip]) loginAttempts[ip] = [];
    loginAttempts[ip] = loginAttempts[ip].filter(function(t) { return now - t < 300000; });
    if (loginAttempts[ip].length >= 10) return false;
    loginAttempts[ip].push(now);
    return true;
}
setInterval(function() { var now = Date.now(); for (var ip in loginAttempts) { loginAttempts[ip] = loginAttempts[ip].filter(function(t) { return now - t < 300000; }); if (loginAttempts[ip].length === 0) delete loginAttempts[ip]; } }, 600000);

// ── 登录 ──
app.post('/api/auth/login', (req, res) => {
    const clientIp = req.ip || req.connection.remoteAddress || 'unknown';
    if (!checkLoginRate(clientIp)) return res.json(err('登录尝试过于频繁，请5分钟后再试'));
    try {"""
if old4 in c:
    c = c.replace(old4, new4, 1)
    fixes += 1
    print('4. login rate limit: FIXED')
else:
    print('4. login rate limit: pattern not found')

with open('/opt/laileme-api/server.js', 'w') as f:
    f.write(c)

print('Total fixes: %d' % fixes)
print('DONE')
