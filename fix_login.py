with open('/opt/laileme-api/server.js', 'r') as f:
    c = f.read()

old4 = u"// \u2500\u2500 \u767b\u5f55 \u2500\u2500\napp.post('/api/auth/login', (req, res) => {\n    try {"
new4 = u"""// \u2500\u2500 \u7b80\u6613\u767b\u5f55\u9650\u6d41 \u2500\u2500
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

// \u2500\u2500 \u767b\u5f55 \u2500\u2500
app.post('/api/auth/login', (req, res) => {
    const clientIp = req.ip || req.connection.remoteAddress || 'unknown';
    if (!checkLoginRate(clientIp)) return res.json(err('\u767b\u5f55\u5c1d\u8bd5\u8fc7\u4e8e\u9891\u7e41\uff0c\u8bf75\u5206\u949f\u540e\u518d\u8bd5'));
    try {"""

if old4 in c:
    c = c.replace(old4, new4, 1)
    print('login rate limit: FIXED')
else:
    print('pattern not found')
    # debug
    idx = c.find("api/auth/login")
    if idx >= 0:
        print('Found at', idx)
        print(repr(c[idx-60:idx+80]))

with open('/opt/laileme-api/server.js', 'w') as f:
    f.write(c)

# verify syntax
import subprocess
r = subprocess.run(['node', '-c', '/opt/laileme-api/server.js'], capture_output=True, text=True)
print('syntax check:', r.stdout.strip(), r.stderr.strip())
print('DONE')
