@echo off
REM 啟動腳本 for Voice Chat Gemini Application (Windows)

echo 🚀 啟動 Voice Chat Gemini 應用程式...

REM 檢查 Docker 是否已安裝
docker --version >nul 2>&1
if %errorlevel% neq 0 (
    echo ❌ Docker 未安裝，請先安裝 Docker Desktop
    pause
    exit /b 1
)

REM 檢查 Docker Compose 是否已安裝
docker-compose --version >nul 2>&1
if %errorlevel% neq 0 (
    docker compose version >nul 2>&1
    if %errorlevel% neq 0 (
        echo ❌ Docker Compose 未安裝，請先安裝 Docker Compose
        pause
        exit /b 1
    )
)

REM 檢查 .env 文件是否存在
if not exist .env (
    echo ⚠️  .env 文件不存在，請確保已正確配置環境變量
    echo 特別是 GOOGLE_API_KEY 需要設置為您的 Gemini API 金鑰
    pause
    exit /b 1
)

REM 創建必要的目錄
echo 📁 創建必要的目錄...
if not exist data mkdir data
if not exist audio mkdir audio
if not exist logs mkdir logs

REM 構建並啟動服務
echo 🔨 構建 Docker 映像...
docker-compose build

echo 🌟 啟動服務...
docker-compose up -d

REM 等待服務啟動
echo ⏳ 等待服務啟動...
timeout /t 10 /nobreak >nul

REM 檢查服務狀態
echo 🔍 檢查服務狀態...
docker-compose ps

REM 顯示日誌
echo 📝 顯示應用程式日誌...
docker-compose logs voice-chat-app

echo.
echo ✅ 應用程式已啟動！
echo 🌐 API 文檔: http://localhost:8000/docs
echo 📊 Redis: localhost:6379
echo.
echo 📋 常用命令:
echo   查看日誌: docker-compose logs -f voice-chat-app
echo   停止服務: docker-compose down
echo   重啟服務: docker-compose restart
echo   進入容器: docker-compose exec voice-chat-app bash
echo.
pause
