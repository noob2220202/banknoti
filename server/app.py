from flask import Flask, request, jsonify
from functools import wraps
import sqlite3, hmac, hashlib, time, os, secrets

app = Flask(__name__)

# ─── 설정 ───────────────────────────────────────────
ADMIN_TOKEN  = os.environ.get("ADMIN_TOKEN",  "change-this-admin-token")
SECRET_HMAC  = os.environ.get("SECRET_HMAC",  "NB-NotifyBridge-2024-K9xP2mQr")
DB_PATH      = os.environ.get("DB_PATH",      "licenses.db")
# ────────────────────────────────────────────────────

def get_db():
    conn = sqlite3.connect(DB_PATH)
    conn.row_factory = sqlite3.Row
    return conn

def init_db():
    with get_db() as db:
        db.execute("""
            CREATE TABLE IF NOT EXISTS licenses (
                key        TEXT PRIMARY KEY,
                days       INTEGER NOT NULL,
                memo       TEXT DEFAULT '',
                used       INTEGER DEFAULT 0,
                device_id  TEXT DEFAULT NULL,
                created_at INTEGER NOT NULL,
                used_at    INTEGER DEFAULT NULL
            )
        """)
        db.commit()

def require_admin(f):
    @wraps(f)
    def decorated(*args, **kwargs):
        token = request.headers.get("X-Admin-Token") or request.json.get("admin_token", "")
        if not secrets.compare_digest(token, ADMIN_TOKEN):
            return jsonify({"error": "unauthorized"}), 401
        return f(*args, **kwargs)
    return decorated

def make_key(days: int, offset: int = 0) -> str:
    expiry = int(time.time()) + days * 86400 + offset
    data   = str(expiry)
    sig    = hmac.new(SECRET_HMAC.encode(), data.encode(), hashlib.sha256).hexdigest()[:12].upper()
    return f"{data}-{sig}"


# ─── API ─────────────────────────────────────────────

@app.get("/api/status")
def status():
    return jsonify({"status": "ok", "service": "NotifyBridge License Server"})


@app.post("/api/generate")
@require_admin
def generate():
    """라이선스 키 생성 (어드민 전용)"""
    data     = request.json or {}
    days     = int(data.get("days", 30))
    quantity = min(int(data.get("quantity", 1)), 100)
    memo     = data.get("memo", "")

    keys = []
    now  = int(time.time())
    with get_db() as db:
        for i in range(quantity):
            key = make_key(days, i)
            db.execute(
                "INSERT OR IGNORE INTO licenses (key, days, memo, created_at) VALUES (?, ?, ?, ?)",
                (key, days, memo, now)
            )
            keys.append(key)
        db.commit()

    return jsonify({
        "keys":       keys,
        "days":       days,
        "quantity":   quantity,
        "memo":       memo,
        "expires_in": f"{days}일"
    })


@app.post("/api/activate")
def activate():
    """키 활성화 (1회용 — 원자적 소모)"""
    data      = request.json or {}
    key       = data.get("key", "").strip()
    device_id = data.get("device_id", "unknown")

    if not key:
        return jsonify({"success": False, "reason": "key_empty"}), 400

    with get_db() as db:
        # 원자적 UPDATE: used=0 인 경우에만 성공
        cur = db.execute("""
            UPDATE licenses
            SET used=1, device_id=?, used_at=?
            WHERE key=? AND used=0
        """, (device_id, int(time.time()), key))
        db.commit()

        if cur.rowcount == 0:
            # 업데이트된 행 없음 → 존재 여부 확인
            row = db.execute("SELECT used FROM licenses WHERE key=?", (key,)).fetchone()
            if row is None:
                return jsonify({"success": False, "reason": "not_found"}), 404
            else:
                return jsonify({"success": False, "reason": "already_used"}), 409

        # 성공 — 만료 시각 반환
        row = db.execute("SELECT days, created_at FROM licenses WHERE key=?", (key,)).fetchone()
        expiry = row["created_at"] + row["days"] * 86400
        return jsonify({"success": True, "expires_at": expiry})


@app.get("/api/keys")
@require_admin
def list_keys():
    """전체 키 목록 조회 (어드민 전용)"""
    page  = int(request.args.get("page", 1))
    limit = min(int(request.args.get("limit", 50)), 200)
    used  = request.args.get("used")

    query = "SELECT * FROM licenses"
    params = []
    if used == "0":
        query += " WHERE used=0"
    elif used == "1":
        query += " WHERE used=1"
    query += " ORDER BY created_at DESC LIMIT ? OFFSET ?"
    params += [limit, (page - 1) * limit]

    with get_db() as db:
        rows = db.execute(query, params).fetchall()
        total = db.execute("SELECT COUNT(*) FROM licenses").fetchone()[0]

    return jsonify({
        "total": total,
        "keys": [dict(r) for r in rows]
    })


@app.delete("/api/keys/<key>")
@require_admin
def delete_key(key):
    """키 삭제 (어드민 전용)"""
    with get_db() as db:
        db.execute("DELETE FROM licenses WHERE key=?", (key,))
        db.commit()
    return jsonify({"success": True})


if __name__ == "__main__":
    init_db()
    app.run(host="0.0.0.0", port=6000, debug=False)
