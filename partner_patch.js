// ── 伴侣模式开关 ──
app.post("/api/partner/mode", verifyToken, (req, res) => {
    try {
        const { enabled } = req.body;
        const db = loadDB();
        const me = db.users.find(u => u.id === req.userId);
        if (!me) return res.json(err("用户不存在"));
        me.partnerMode = !!enabled;
        saveDB(db);
        res.json(ok({ partnerMode: me.partnerMode }, enabled ? "伴侣模式已开启" : "伴侣模式已关闭"));
    } catch (e) {
        console.error("伴侣模式错误:", e);
        res.json(err("操作失败"));
    }
});

// ── 发送绑定请求（不直接绑定）──
app.post("/api/partner/request", verifyToken, (req, res) => {
    try {
        const { partnerUid } = req.body;
        if (!partnerUid) return res.json(err("请输入伴侣的UID"));
        const db = loadDB();
        const me = db.users.find(u => u.id === req.userId);
        if (!me) return res.json(err("用户不存在"));
        if (me.partnerId) return res.json(err("你已绑定伴侣，请先解绑"));
        const partnerId = parseInt(partnerUid, 10);
        if (isNaN(partnerId)) return res.json(err("无效的UID"));
        if (partnerId === req.userId) return res.json(err("不能绑定自己哦"));
        const partner = db.users.find(u => u.id === partnerId);
        if (!partner) return res.json(err("未找到该用户"));
        // 检查对方是否开启了伴侣模式
        if (!partner.partnerMode) return res.json(err("对方未开启伴侣模式，无法发送请求"));
        // 检查对方是否已被绑定
        if (partner.partnerId) return res.json(err("对方已绑定伴侣"));
        // 检查是否已发送过请求
        if (!partner.partnerRequests) partner.partnerRequests = [];
        const exists = partner.partnerRequests.find(r => r.fromId === req.userId);
        if (exists) return res.json(err("已发送过请求，请等待对方确认"));
        // 添加请求
        const myUidStr = String(req.userId).padStart(Math.max(5, String(req.userId).length), "0");
        partner.partnerRequests.push({
            fromId: req.userId,
            fromUid: myUidStr,
            fromNickname: me.nickname || me.username,
            fromAvatarUrl: me.avatarUrl || "",
            fromGender: me.gender || "male",
            timestamp: Date.now()
        });
        saveDB(db);
        res.json(ok(null, "绑定请求已发送，等待对方确认"));
    } catch (e) {
        console.error("发送绑定请求错误:", e);
        res.json(err("发送失败"));
    }
});

// ── 获取收到的绑定请求 ──
app.get("/api/partner/pending", verifyToken, (req, res) => {
    try {
        const db = loadDB();
        const me = db.users.find(u => u.id === req.userId);
        if (!me) return res.json(err("用户不存在"));
        res.json(ok({
            partnerMode: !!me.partnerMode,
            requests: me.partnerRequests || []
        }));
    } catch (e) {
        console.error("获取绑定请求错误:", e);
        res.json(err("获取失败"));
    }
});

// ── 同意绑定请求 ──
app.post("/api/partner/accept", verifyToken, (req, res) => {
    try {
        const { fromId } = req.body;
        if (!fromId) return res.json(err("缺少请求ID"));
        const db = loadDB();
        const me = db.users.find(u => u.id === req.userId);
        if (!me) return res.json(err("用户不存在"));
        if (me.partnerId) return res.json(err("你已绑定伴侣，请先解绑"));
        if (!me.partnerRequests) return res.json(err("没有待处理的请求"));
        const reqIdx = me.partnerRequests.findIndex(r => r.fromId === fromId);
        if (reqIdx === -1) return res.json(err("该请求不存在"));
        const requester = db.users.find(u => u.id === fromId);
        if (!requester) {
            me.partnerRequests.splice(reqIdx, 1);
            saveDB(db);
            return res.json(err("请求方账号已不存在"));
        }
        if (requester.partnerId) {
            me.partnerRequests.splice(reqIdx, 1);
            saveDB(db);
            return res.json(err("对方已绑定了其他伴侣"));
        }
        // 双向绑定
        me.partnerId = fromId;
        requester.partnerId = req.userId;
        // 清空所有待处理请求
        me.partnerRequests = [];
        saveDB(db);
        const partnerUidStr = String(fromId).padStart(Math.max(5, String(fromId).length), "0");
        res.json(ok({
            partnerId: String(fromId),
            partnerUid: partnerUidStr,
            partnerNickname: requester.nickname || requester.username,
            partnerAvatarUrl: requester.avatarUrl || "",
            partnerGender: requester.gender || "male"
        }, "绑定成功"));
    } catch (e) {
        console.error("同意绑定错误:", e);
        res.json(err("操作失败"));
    }
});

// ── 拒绝绑定请求 ──
app.post("/api/partner/reject", verifyToken, (req, res) => {
    try {
        const { fromId } = req.body;
        if (!fromId) return res.json(err("缺少请求ID"));
        const db = loadDB();
        const me = db.users.find(u => u.id === req.userId);
        if (!me) return res.json(err("用户不存在"));
        if (!me.partnerRequests) return res.json(err("没有待处理的请求"));
        me.partnerRequests = me.partnerRequests.filter(r => r.fromId !== fromId);
        saveDB(db);
        res.json(ok(null, "已拒绝"));
    } catch (e) {
        console.error("拒绝绑定错误:", e);
        res.json(err("操作失败"));
    }
});

// ── 解绑伴侣 ──
app.post("/api/partner/unbind", verifyToken, (req, res) => {
    try {
        const db = loadDB();
        const me = db.users.find(u => u.id === req.userId);
        if (!me) return res.json(err("用户不存在"));
        if (!me.partnerId) return res.json(err("你还没有绑定伴侣"));
        const partner = db.users.find(u => u.id === me.partnerId);
        if (partner && partner.partnerId === req.userId) {
            delete partner.partnerId;
        }
        delete me.partnerId;
        saveDB(db);
        res.json(ok(null, "已解绑伴侣"));
    } catch (e) {
        console.error("解绑伴侣错误:", e);
        res.json(err("解绑失败"));
    }
});

// ── 获取伴侣信息 ──
app.get("/api/partner/info", verifyToken, (req, res) => {
    try {
        const db = loadDB();
        const me = db.users.find(u => u.id === req.userId);
        if (!me) return res.json(err("用户不存在"));
        if (!me.partnerId) return res.json(ok({ bound: false, partnerMode: !!me.partnerMode }, "未绑定伴侣"));
        const partner = db.users.find(u => u.id === me.partnerId);
        if (!partner) {
            delete me.partnerId;
            saveDB(db);
            return res.json(ok({ bound: false, partnerMode: !!me.partnerMode }, "伴侣账号已不存在"));
        }
        const partnerUidStr = String(partner.id).padStart(Math.max(5, String(partner.id).length), "0");
        res.json(ok({
            bound: true,
            partnerMode: !!me.partnerMode,
            partnerId: String(partner.id),
            partnerUid: partnerUidStr,
            partnerNickname: partner.nickname || partner.username,
            partnerAvatarUrl: partner.avatarUrl || "",
            partnerGender: partner.gender || "female"
        }));
    } catch (e) {
        console.error("获取伴侣信息错误:", e);
        res.json(err("获取失败"));
    }
});

// ── 获取伴侣的经期和同步数据 ──
app.get("/api/partner/data", verifyToken, (req, res) => {
    try {
        const db = loadDB();
        const me = db.users.find(u => u.id === req.userId);
        if (!me) return res.json(err("用户不存在"));
        if (!me.partnerId) return res.json(err("未绑定伴侣"));
        const partner = db.users.find(u => u.id === me.partnerId);
        if (!partner) return res.json(err("伴侣账号不存在"));
        const syncData = loadUserSync(me.partnerId);
        res.json(ok({
            partnerNickname: partner.nickname || partner.username,
            periodRecords: syncData.periodRecords || [],
            diaryEntries: [],
            sleepRecords: syncData.sleepRecords || [],
            healthData: syncData.healthData || {},
            lastSync: syncData.lastSync || null
        }));
    } catch (e) {
        console.error("获取伴侣数据错误:", e);
        res.json(err("获取失败"));
    }
});

// ── 发送关怀给伴侣 ──
app.post("/api/partner/care", verifyToken, (req, res) => {
    try {
        const db = loadDB();
        const me = db.users.find(u => u.id === req.userId);
        if (!me) return res.json(err("用户不存在"));
        if (!me.partnerId) return res.json(err("未绑定伴侣，无法发送关怀"));
        const partner = db.users.find(u => u.id === me.partnerId);
        if (!partner) return res.json(err("伴侣账号不存在"));

        // 记录关怀到伴侣的 careReceived 列表
        if (!partner.careReceived) partner.careReceived = [];
        partner.careReceived.push({
            fromId: req.userId,
            fromNickname: me.nickname || me.username,
            timestamp: Date.now(),
            read: false
        });
        // 只保留最近 50 条
        if (partner.careReceived.length > 50) {
            partner.careReceived = partner.careReceived.slice(-50);
        }
        saveDB(db);
        res.json(ok(null, "关怀已送达 💕"));
    } catch (e) {
        console.error("发送关怀错误:", e);
        res.json(err("发送失败"));
    }
});

// ── 获取收到的关怀（女方查看）──
app.get("/api/partner/care", verifyToken, (req, res) => {
    try {
        const db = loadDB();
        const me = db.users.find(u => u.id === req.userId);
        if (!me) return res.json(err("用户不存在"));
        const cares = me.careReceived || [];
        // 返回未读关怀
        const unread = cares.filter(c => !c.read);
        res.json(ok({
            total: cares.length,
            unread: unread.length,
            messages: cares.slice(-20).reverse()
        }));
    } catch (e) {
        console.error("获取关怀错误:", e);
        res.json(err("获取失败"));
    }
});

// ── 标记关怀已读 ──
app.post("/api/partner/care/read", verifyToken, (req, res) => {
    try {
        const db = loadDB();
        const me = db.users.find(u => u.id === req.userId);
        if (!me) return res.json(err("用户不存在"));
        if (me.careReceived) {
            me.careReceived.forEach(c => { c.read = true; });
            saveDB(db);
        }
        res.json(ok(null, "已标记全部已读"));
    } catch (e) {
        console.error("标记已读错误:", e);
        res.json(err("操作失败"));
    }
});

// ══════════════════════════════════════════
// 公告系统
// ══════════════════════════════════════════

// ── 获取公告列表 ──
app.get("/api/announcements", (req, res) => {
    try {
        const db = loadDB();
        const announcements = db.announcements || [];
        // 按时间倒序，返回最近 20 条
        const sorted = announcements
            .sort((a, b) => (b.createdAt || 0) - (a.createdAt || 0))
            .slice(0, 20);
        res.json(ok(sorted));
    } catch (e) {
        console.error("获取公告错误:", e);
        res.json(ok([]));
    }
});

// ── 发布公告（管理接口，可选加密码保护）──
app.post("/api/announcements", (req, res) => {
    try {
        const { title, content, adminKey } = req.body;
        // 简单的管理密钥校验
        if (adminKey !== "laileme_admin_2024") {
            return res.json(err("无权限"));
        }
        if (!title || !content) return res.json(err("标题和内容不能为空"));
        const db = loadDB();
        if (!db.announcements) db.announcements = [];
        const now = new Date();
        const timeStr = `${now.getFullYear()}-${String(now.getMonth()+1).padStart(2,'0')}-${String(now.getDate()).padStart(2,'0')} ${String(now.getHours()).padStart(2,'0')}:${String(now.getMinutes()).padStart(2,'0')}`;
        db.announcements.push({
            id: Date.now(),
            title,
            content,
            time: timeStr,
            createdAt: Date.now()
        });
        saveDB(db);
        res.json(ok(null, "公告发布成功"));
    } catch (e) {
        console.error("发布公告错误:", e);
        res.json(err("发布失败"));
    }
});

// ══════════════════════════════════════════
// 心跳与数据保留策略
// ══════════════════════════════════════════

const DAY_MS = 24 * 60 * 60 * 1000;

// 数据保留天数配置
const RETENTION_DAYS = {
    free: 60,          // 免费用户：不活跃后60天
    monthly_vip: 120,  // 月度VIP：到期后120天
    yearly_vip: 720,   // 年度VIP：到期后720天
    permanent_vip: -1  // 永久VIP：永不删除（-1表示永久）
};

// ── 心跳接口（客户端每次同步时调用）──
app.post("/api/heartbeat", verifyToken, (req, res) => {
    try {
        const db = loadDB();
        const user = db.users.find(u => u.id === req.userId);
        if (!user) return res.json(err("用户不存在"));
        user.lastActive = Date.now();
        saveDB(db);
        res.json(ok({ lastActive: user.lastActive }));
    } catch (e) {
        console.error("心跳错误:", e);
        res.json(err("心跳失败"));
    }
});

// ── 查询用户数据保留状态（客户端可调用查看）──
app.get("/api/data-retention/status", verifyToken, (req, res) => {
    try {
        const db = loadDB();
        const user = db.users.find(u => u.id === req.userId);
        if (!user) return res.json(err("用户不存在"));

        const vipType = getUserVipType(user);
        const retentionDays = RETENTION_DAYS[vipType];
        const lastActive = user.lastActive || Date.now();
        const now = Date.now();

        let expiresAt = null;
        let daysRemaining = null;

        if (retentionDays === -1) {
            // 永久VIP，永不过期
            expiresAt = null;
            daysRemaining = -1;
        } else if (vipType === "free") {
            // 免费用户：从最后活跃时间算起
            expiresAt = lastActive + retentionDays * DAY_MS;
            daysRemaining = Math.max(0, Math.ceil((expiresAt - now) / DAY_MS));
        } else {
            // 付费VIP：从VIP到期时间算起
            const vipExpiry = user.vipExpiry || now;
            if (vipExpiry > now) {
                // VIP未过期，数据安全
                expiresAt = vipExpiry + retentionDays * DAY_MS;
                daysRemaining = Math.max(0, Math.ceil((expiresAt - now) / DAY_MS));
            } else {
                // VIP已过期
                expiresAt = vipExpiry + retentionDays * DAY_MS;
                daysRemaining = Math.max(0, Math.ceil((expiresAt - now) / DAY_MS));
            }
        }

        res.json(ok({
            vipType,
            retentionDays: retentionDays === -1 ? "永久" : retentionDays,
            lastActive: new Date(lastActive).toISOString(),
            expiresAt: expiresAt ? new Date(expiresAt).toISOString() : null,
            daysRemaining,
            isActive: (now - lastActive) < 60 * DAY_MS
        }));
    } catch (e) {
        console.error("查询保留状态错误:", e);
        res.json(err("查询失败"));
    }
});

// ── 判断用户VIP类型 ──
function getUserVipType(user) {
    if (!user.vipType && !user.isVip) return "free";
    const vipType = (user.vipType || "").toLowerCase();
    if (vipType === "permanent" || vipType === "永久") return "permanent_vip";
    if (vipType === "yearly" || vipType === "年度") return "yearly_vip";
    if (vipType === "monthly" || vipType === "月度") return "monthly_vip";
    // 兼容旧数据：有isVip但没vipType的情况
    if (user.isVip) return "monthly_vip";
    return "free";
}

// ── 数据清理定时任务（每天凌晨3点执行）──
function runDataCleanup() {
    try {
        const db = loadDB();
        const now = Date.now();
        let cleanedCount = 0;

        for (const user of db.users) {
            const vipType = getUserVipType(user);
            const retentionDays = RETENTION_DAYS[vipType];

            // 永久VIP永不清除
            if (retentionDays === -1) continue;

            let shouldClean = false;

            if (vipType === "free") {
                // 免费用户：最后活跃 + 60天
                const lastActive = user.lastActive || user.createdAt || 0;
                if (lastActive > 0 && (now - lastActive) > retentionDays * DAY_MS) {
                    shouldClean = true;
                }
            } else {
                // 付费VIP（月度/年度）：VIP到期 + 保留天数
                const vipExpiry = user.vipExpiry || 0;
                if (vipExpiry > 0 && vipExpiry < now) {
                    // VIP已过期
                    if ((now - vipExpiry) > retentionDays * DAY_MS) {
                        shouldClean = true;
                    }
                }
            }

            if (shouldClean) {
                // 删除用户的同步数据文件
                const syncFile = `./data/sync_${user.id}.json`;
                const fs = require("fs");
                if (fs.existsSync(syncFile)) {
                    fs.unlinkSync(syncFile);
                    console.log(`[数据清理] 已删除用户 ${user.id}(${user.username}) 的同步数据, VIP类型: ${vipType}`);
                    cleanedCount++;
                }
                // 标记数据已清除
                user.dataCleared = true;
                user.dataClearedAt = now;
            }
        }

        if (cleanedCount > 0) {
            saveDB(db);
            console.log(`[数据清理] 本次共清理 ${cleanedCount} 个用户的数据`);
        } else {
            console.log("[数据清理] 本次无需清理");
        }
    } catch (e) {
        console.error("[数据清理] 执行失败:", e);
    }
}

// 每天凌晨3点执行清理
function scheduleCleanup() {
    const now = new Date();
    const next3am = new Date();
    next3am.setHours(3, 0, 0, 0);
    if (next3am <= now) next3am.setDate(next3am.getDate() + 1);
    const delay = next3am.getTime() - now.getTime();

    setTimeout(() => {
        runDataCleanup();
        // 之后每24小时执行一次
        setInterval(runDataCleanup, 24 * 60 * 60 * 1000);
    }, delay);

    console.log(`[数据清理] 定时任务已设置，下次执行: ${next3am.toLocaleString()}`);
}

// 启动清理定时任务
scheduleCleanup();

// ── 管理接口：手动触发清理（调试用）──
app.post("/api/admin/cleanup", (req, res) => {
    const { adminKey } = req.body;
    if (adminKey !== "laileme_admin_2024") return res.json(err("无权限"));
    runDataCleanup();
    res.json(ok(null, "手动清理已执行"));
});

// ── 管理接口：查看所有用户活跃状态 ──
app.post("/api/admin/user-activity", (req, res) => {
    const { adminKey } = req.body;
    if (adminKey !== "laileme_admin_2024") return res.json(err("无权限"));
    try {
        const db = loadDB();
        const now = Date.now();
        const users = db.users.map(u => {
            const vipType = getUserVipType(u);
            const lastActive = u.lastActive || 0;
            const inactiveDays = lastActive > 0 ? Math.floor((now - lastActive) / DAY_MS) : -1;
            return {
                id: u.id,
                username: u.username,
                nickname: u.nickname,
                vipType,
                lastActive: lastActive > 0 ? new Date(lastActive).toISOString() : "从未活跃",
                inactiveDays,
                dataCleared: !!u.dataCleared
            };
        });
        res.json(ok({ total: users.length, users }));
    } catch (e) {
        res.json(err("查询失败"));
    }
});
