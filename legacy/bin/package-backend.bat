@echo off

rem 后端打包脚本
rem 用法: package-backend.bat [dev|prod]

set ENV=%1
if "%ENV%"=="" set ENV=dev

echo 开始打包后端项目，环境: %ENV%

cd /d "%~dp0..\hm-admin"
mvn clean package -P%ENV%

if %ERRORLEVEL% equ 0 (
    echo 后端打包成功！
    echo 打包文件位于: %~dp0..\hm-admin\target\hm-admin.war
) else (
    echo 后端打包失败！
    pause
    exit /b 1
)

pause