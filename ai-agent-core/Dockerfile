# 1. 找一个带 Java 17 的极其精简的 Linux 底座（只有 5MB 大小）
FROM eclipse-temurin:17-jre-alpine

# 2. 在太空舱里建一个工作目录
WORKDIR /app

# 3. 把你打好的 jar 包复制到太空舱里，并改名叫 app.jar
# （注意：前面这个名字要改成你 target 目录里实际产生的 jar 包名字）
COPY target/ai-agent-core-0.0.1-SNAPSHOT.jar app.jar

# 4. 声明这个舱体会对外暴露 8080 端口
EXPOSE 8080

# 5. 太空舱启动时，自动执行这行命令跑起你的项目
ENTRYPOINT ["java", "-jar", "app.jar"]