@echo off

rem 前端打包脚本
rem 用法: package-frontend.bat [dev|test|prod]

set ENV=%1
if "%ENV%"=="" set ENV=dev

echo 开始打包前端项目，环境: %ENV%

cd /d "%~dp0..\hm-ui"

if "%ENV%"=="dev" (
    npm run build:prod
) else if "%ENV%"=="test" (
    npm run build:stage
) else if "%ENV%"=="prod" (
    npm run build:prod
) else (
    echo 无效的环境参数，默认使用生产环境
    npm run build:prod
)

if %ERRORLEVEL% equ 0 (
    echo 前端打包成功！
    echo 打包文件位于: %~dp0..\hm-ui\dist
) else (
    echo 前端打包失败！
    pause
    exit /b 1
)

pause