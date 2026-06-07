#!/usr/bin/env python3
"""
라이선스 키 생성 CLI — VPS 터미널에서 직접 실행
사용법: python3 keygen.py [일수] [개수] [메모]
예시:
  python3 keygen.py 7         # 7일짜리 1개
  python3 keygen.py 30 5      # 30일짜리 5개
  python3 keygen.py 1 3 홍길동  # 1일짜리 3개, 메모=홍길동
"""

import sys, os, sqlite3, hmac, hashlib, time
from datetime import datetime, timedelta, timezone

# ── 설정 (.env 또는 환경변수에서 읽기) ─────────────────────────
def load_env(path="/opt/notifybridge/.env"):
    if os.path.exists(path):
        for line in open(path):
            line = line.strip()
            if line and not line.startswith("#") and "=" in line:
                k, v = line.split("=", 1)
                os.environ.setdefault(k.strip(), v.strip().strip('"').strip("'"))

load_env()

SECRET_HMAC = os.environ.get("SECRET_HMAC", "NB-NotifyBridge-2024-K9xP2mQr")
DB_PATH     = os.environ.get("DB_PATH",     "/opt/notifybridge/licenses.db")

# ── 키 생성 ─────────────────────────────────────────────────────
def make_key(days: int, offset: int = 0) -> str:
    expiry = int(time.time()) + days * 86400 + offset
    sig    = hmac.new(SECRET_HMAC.encode(), str(expiry).encode(), hashlib.sha256).hexdigest()[:12].upper()
    return f"{expiry}-{sig}"

def generate(days: int, quantity: int = 1, memo: str = "") -> list[str]:
    conn = sqlite3.connect(DB_PATH)
    conn.row_factory = sqlite3.Row
    conn.execute("""
        CREATE TABLE IF NOT EXISTS licenses (
            key TEXT PRIMARY KEY, days INTEGER NOT NULL,
            memo TEXT DEFAULT '', used INTEGER DEFAULT 0,
            device_id TEXT DEFAULT NULL,
            created_at INTEGER NOT NULL, used_at INTEGER DEFAULT NULL
        )
    """)
    now  = int(time.time())
    keys = []
    for i in range(quantity):
        key = make_key(days, i)
        conn.execute(
            "INSERT OR IGNORE INTO licenses (key, days, memo, created_at) VALUES (?,?,?,?)",
            (key, days, memo, now)
        )
        keys.append(key)
    conn.commit()
    conn.close()
    return keys

# ── 메인 ─────────────────────────────────────────────────────────
def main():
    args = sys.argv[1:]

    # 대화형 모드
    if not args:
        print("=" * 50)
        print("  NotifyBridge 라이선스 키 생성기")
        print("=" * 50)
        try:
            days_input = input("유효 기간 (1/7/30/9999 또는 직접 입력) [30]: ").strip()
            days = int(days_input) if days_input else 30
            qty_input = input("생성 개수 [1]: ").strip()
            qty = int(qty_input) if qty_input else 1
            memo = input("메모 (선택, 수신자 이름 등) [없음]: ").strip()
        except (ValueError, KeyboardInterrupt):
            print("\n취소되었습니다.")
            sys.exit(0)
    else:
        try:
            days = int(args[0])
            qty  = int(args[1]) if len(args) > 1 else 1
            memo = args[2] if len(args) > 2 else ""
        except ValueError:
            print("사용법: python3 keygen.py [일수] [개수] [메모]")
            sys.exit(1)

    if days <= 0 or qty <= 0 or qty > 100:
        print("오류: 일수는 1 이상, 개수는 1~100 사이여야 합니다.")
        sys.exit(1)

    keys = generate(days, qty, memo)

    exp = (datetime.now(timezone.utc) + timedelta(days=days)).strftime("%Y-%m-%d")

    print()
    print("=" * 50)
    print(f"  ✅ 키 {len(keys)}개 발급 완료")
    print(f"  유효 기간 : {days}일  (만료: {exp})")
    if memo:
        print(f"  메모      : {memo}")
    print("=" * 50)
    for key in keys:
        print(f"  {key}")
    print("=" * 50)
    print()

if __name__ == "__main__":
    main()
