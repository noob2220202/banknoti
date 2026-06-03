#!/usr/bin/env python3
"""
NotifyBridge 라이선스 키 생성기
사용법: python3 generate_license.py
"""
import hmac
import hashlib
import time

# ⚠️ LicenseManager.kt의 SECRET과 반드시 동일해야 함
SECRET = "NB-NotifyBridge-2024-K9xP2mQr"

DURATIONS = {
    "1일":    1,
    "7일":    7,
    "30일":   30,
    "9999일": 9999,
}

def generate(days: int) -> str:
    expiry = int(time.time()) + days * 86400
    data = str(expiry)
    sig = hmac.new(SECRET.encode(), data.encode(), hashlib.sha256).hexdigest()[:12].upper()
    return f"{data}-{sig}"

def validate(key: str) -> bool:
    parts = key.strip().split("-")
    if len(parts) != 2:
        return False
    try:
        expiry = int(parts[0])
    except ValueError:
        return False
    sig = parts[1]
    expected = hmac.new(SECRET.encode(), parts[0].encode(), hashlib.sha256).hexdigest()[:12].upper()
    return sig == expected and expiry > int(time.time())

if __name__ == "__main__":
    print("=" * 50)
    print("  NotifyBridge 라이선스 키 생성기")
    print("=" * 50)
    print()

    for label, days in DURATIONS.items():
        key = generate(days)
        print(f"[{label:>6}]  {key}")

    print()
    print("=" * 50)
    print("특정 기간 직접 지정:")
    try:
        days_input = int(input("일수 입력 (예: 14): "))
        key = generate(days_input)
        print(f"[{days_input}일]  {key}")
    except (ValueError, EOFError):
        pass
