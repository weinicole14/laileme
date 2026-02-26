// ──────────────── 邀请有礼 服务端补丁 ────────────────
// 规则：
//   邀请者：每邀请1人 → 30天VIP；累计邀请满10人 → 永久VIP
//   被邀请者：使用邀请码 → 30天VIP
//   每人只能使用一次邀请码，不能用自己的

// ── 生成邀请码工具函数 ──
function generateInviteCode(userId) {
    const chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    let code = "";
    for (let i = 0; i < 6; i++) {
        code += chars.charAt(Math.floor(Math.random() * chars.length));
    }
    return code;
}

// ── 给用户添加VIP天数（或永久） ──
function grantVip(user, days, reason) {
    if (!user.vip) user.vip = {};
    
    // 永久VIP
    if (days === -1) {
        user.vip.permanent = true;
        user.vip.source = reason;
        console.log(`[邀请] 用户${user.id} 获得永久VIP (${reason})`);
        return;
    }
    
    // 计算到期时间
    const now = Date.now();
    const currentExpiry = user.vip.expiresAt || now;
    const base = Math.max(currentExpiry, now); // 如果已有VIP则往后叠加
    user.vip.expiresAt = base + days * 24 * 60 * 60 * 1000;
    user.vip.source = reason;
    console.log(`[邀请] 用户${user.id} 获得${days}天VIP (${reason})，到期：${new Date(user.vip.expiresAt).toISOString()}`);
}

// ── 获取我的邀请码和统计 ──
app.get("/api/invite/my-code", verifyToken, (req, res) => {
    try {
        const db = loadDB();
        const me = db.users.find(u => u.id === req.userId);
        if (!me) return res.json(err("用户不存在"));

        // 如果还没有邀请码，生成一个
        if (!me.inviteCode) {
            // 确保不重复
            let code;
            do {
                code = generateInviteCode(me.id);
            } while (db.users.some(u => u.inviteCode === code));
            me.inviteCode = code;
            saveDB(db);
        }

        // 统计邀请人数和奖励天数
        if (!me.inviteRecords) me.inviteRecords = [];
        const invitedCount = me.inviteRecords.length;
        const totalRewardDays = me.inviteRecords.reduce((sum, r) => sum + (r.rewardDays || 30), 0);

        res.json(ok({
            code: me.inviteCode,
            invitedCount: invitedCount,
            totalRewardDays: totalRewardDays,
            isPermanentVip: !!(me.vip && me.vip.permanent),
            usedCode: me.usedInviteCode || null
        }));
    } catch (e) {
        console.error("获取邀请码错误:", e);
        res.json(err("获取失败"));
    }
});

// ── 使用邀请码（兑换） ──
app.post("/api/invite/redeem", verifyToken, (req, res) => {
    try {
        const { code } = req.body;
        if (!code) return res.json(err("请输入邀请码"));

        const db = loadDB();
        const me = db.users.find(u => u.id === req.userId);
        if (!me) return res.json(err("用户不存在"));

        // 检查是否已使用过邀请码
        if (me.usedInviteCode) {
            return res.json(err("你已经使用过邀请码了"));
        }

        // 查找邀请码的主人
        const inviter = db.users.find(u => u.inviteCode === code.toUpperCase().trim());
        if (!inviter) return res.json(err("邀请码不存在"));

        // 不能用自己的
        if (inviter.id === req.userId) {
            return res.json(err("不能使用自己的邀请码哦"));
        }

        // ── 给被邀请者（我）30天VIP ──
        grantVip(me, 30, "使用邀请码");
        me.usedInviteCode = code.toUpperCase().trim();

        // ── 给邀请者奖励 ──
        if (!inviter.inviteRecords) inviter.inviteRecords = [];
        inviter.inviteRecords.push({
            userId: req.userId,
            username: me.nickname || me.username,
            rewardDays: 30,
            timestamp: Date.now()
        });

        const invitedCount = inviter.inviteRecords.length;

        // 检查是否达到10人 → 永久VIP
        if (invitedCount >= 10 && !(inviter.vip && inviter.vip.permanent)) {
            grantVip(inviter, -1, "邀请满10人");
        } else {
            grantVip(inviter, 30, "邀请奖励");
        }

        saveDB(db);

        res.json(ok({
            rewardDays: 30,
            message: "兑换成功，你获得了30天VIP！"
        }, "兑换成功！你和邀请者都获得了VIP奖励～"));

    } catch (e) {
        console.error("兑换邀请码错误:", e);
        res.json(err("兑换失败"));
    }
});

// ── 开发者创建特殊邀请码（管理员功能） ──
app.post("/api/invite/create-special", (req, res) => {
    try {
        const { code, rewardDays, adminPassword } = req.body;
        if (adminPassword !== "laileme2024admin") {
            return res.json(err("管理员密码错误"));
        }
        if (!code) return res.json(err("请输入邀请码"));

        const db = loadDB();
        // 检查是否已存在
        if (db.users.some(u => u.inviteCode === code.toUpperCase())) {
            return res.json(err("该邀请码已存在"));
        }

        // 存入特殊邀请码列表
        if (!db.specialInviteCodes) db.specialInviteCodes = [];
        db.specialInviteCodes.push({
            code: code.toUpperCase(),
            rewardDays: rewardDays || 30,
            createdAt: Date.now(),
            usedBy: []
        });
        saveDB(db);

        res.json(ok({ code: code.toUpperCase() }, "特殊邀请码创建成功"));
    } catch (e) {
        console.error("创建特殊邀请码错误:", e);
        res.json(err("创建失败"));
    }
});

// ── 检查VIP状态 ──
app.get("/api/invite/vip-status", verifyToken, (req, res) => {
    try {
        const db = loadDB();
        const me = db.users.find(u => u.id === req.userId);
        if (!me) return res.json(err("用户不存在"));

        const vip = me.vip || {};
        const now = Date.now();
        
        let isVip = false;
        let expiresAt = null;
        let permanent = false;

        if (vip.permanent) {
            isVip = true;
            permanent = true;
        } else if (vip.expiresAt && vip.expiresAt > now) {
            isVip = true;
            expiresAt = vip.expiresAt;
        }

        res.json(ok({
            isVip,
            permanent,
            expiresAt,
            source: vip.source || null
        }));
    } catch (e) {
        console.error("获取VIP状态错误:", e);
        res.json(err("获取失败"));
    }
});
