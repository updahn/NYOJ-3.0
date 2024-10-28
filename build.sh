#!/bin/bash

# 一键打包并且复制到本地虚拟机中

# 切换到后端端 进行 Maven 清理和打包
cd /e/NYOJ/NYOJ3.0/hoj-springboot || exit
mvn clean package

# 切换到前端 进行 npm 打包
cd /e/NYOJ/NYOJ3.0/hoj-vue || exit
# 设置环境变量确保 build 不打开浏览器，并完成后自动结束进程
npm run build
