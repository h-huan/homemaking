@echo off

rem 后端运行脚本
rem 用法: run-backend.bat [dev|prod]

set ENV=%1
if "%ENV%"=="" set ENV=dev

echo 开始运行后端项目，环境: %ENV%

cd /d "%~dp0..\hm-admin\target"

if exist "hm-admin.war" (
    java -jar hm-admin.war --spring.profiles.active=%ENV%
) else (
    echo 未找到打包文件，请先运行 package-backend.bat 进行打包
    pause
    exit /b 1
)
