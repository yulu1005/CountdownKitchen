#!/bin/bash

# 啟動腳本 for Voice Chat Gemini Application

echo "🚀 啟動 Voice Chat Gemini 應用程式..."

# 檢查 Docker 是否已安裝
if ! command -v docker &> /dev/null; then
    echo "❌ Docker 未安裝，請先安裝 Docker"
    exit 1
fi

# 檢查 Docker Compose 是否已安裝
if ! command -v docker-compose &> /dev/null && ! docker compose version &> /dev/null; then
    echo "❌ Docker Compose 未安裝，請先安裝 Docker Compose"
    exit 1
fi

# 檢查 .env 文件是否存在
if [ ! -f .env ]; then
    echo "⚠️  .env 文件不存在，請確保已正確配置環境變量"
    echo "特別是 GOOGLE_API_KEY 需要設置為您的 Gemini API 金鑰"
    exit 1
fi

# 創建必要的目錄
echo "📁 創建必要的目錄..."
mkdir -p data
mkdir -p audio
mkdir -p logs

# 構建並啟動服務
echo "🔨 構建 Docker 映像..."
docker-compose build

echo "🌟 啟動服務..."
docker-compose up -d

# 等待服務啟動
echo "⏳ 等待服務啟動..."
sleep 10

# 檢查服務狀態
echo "🔍 檢查服務狀態..."
docker-compose ps

# 顯示日誌
echo "📝 顯示應用程式日誌..."
docker-compose logs voice-chat-app

echo ""
echo "✅ 應用程式已啟動！"
echo "🌐 API 文檔: http://localhost:8000/docs"
echo "📊 Redis: localhost:6379"
echo ""
echo "📋 常用命令:"
echo "  查看日誌: docker-compose logs -f voice-chat-app"
echo "  停止服務: docker-compose down"
echo "  重啟服務: docker-compose restart"
echo "  進入容器: docker-compose exec voice-chat-app bash"
echo ""
