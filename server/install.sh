#!/bin/bash
# NotifyBridge 라이선스 서버 설치 스크립트
# 사용법: bash install.sh

set -e

echo "=== NotifyBridge 라이선스 서버 설치 ==="

# Python 및 pip 확인
if ! command -v python3 &>/dev/null; then
    apt-get update && apt-get install -y python3 python3-pip python3-venv
fi

# 작업 디렉토리
INSTALL_DIR="/opt/notifybridge"
mkdir -p $INSTALL_DIR
cp app.py requirements.txt $INSTALL_DIR/

# 가상환경 생성 및 의존성 설치
cd $INSTALL_DIR
python3 -m venv venv
source venv/bin/activate
pip install -r requirements.txt

# 환경변수 파일 생성 (토큰 직접 입력)
if [ ! -f "$INSTALL_DIR/.env" ]; then
    echo "ADMIN_TOKEN=$(python3 -c 'import secrets; print(secrets.token_hex(32))')" > $INSTALL_DIR/.env
    echo "SECRET_HMAC=$(python3 -c 'import secrets; print(secrets.token_hex(24))')"  >> $INSTALL_DIR/.env
    echo "DB_PATH=$INSTALL_DIR/licenses.db" >> $INSTALL_DIR/.env
    echo ""
    echo "⚠️  .env 파일이 생성되었습니다: $INSTALL_DIR/.env"
    echo "    ADMIN_TOKEN과 SECRET_HMAC 값을 반드시 기록해두세요!"
    echo ""
    cat $INSTALL_DIR/.env
fi

# systemd 서비스 등록
cat > /etc/systemd/system/notifybridge.service << EOF
[Unit]
Description=NotifyBridge License Server
After=network.target

[Service]
User=root
WorkingDirectory=$INSTALL_DIR
EnvironmentFile=$INSTALL_DIR/.env
ExecStart=$INSTALL_DIR/venv/bin/gunicorn -w 2 -b 0.0.0.0:6000 app:app
Restart=always

[Install]
WantedBy=multi-user.target
EOF

systemctl daemon-reload
systemctl enable notifybridge
systemctl start notifybridge

echo ""
echo "✅ 설치 완료!"
echo "   서버 상태: systemctl status notifybridge"
echo "   서버 주소: http://YOUR_VPS_IP:6000"
echo "   API 테스트: curl http://YOUR_VPS_IP:6000/api/status"
