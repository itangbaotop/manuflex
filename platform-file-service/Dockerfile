# 使用轻量级 JDK 17 基础镜像
FROM eclipse-temurin:17-jre

# 设置工作目录
WORKDIR /app

# 将构建好的 jar 包复制到镜像中
# 注意：这里假设你执行过 mvn package，target 下有 jar 包
COPY target/*.jar app.jar

# 具体端口可以在 docker-compose 中覆盖，这里只是声明
EXPOSE 8086

# 启动命令
# 增加 -Djava.security.egd=file:/dev/./urandom 加快启动时的随机数生成
# 增加 JVM 内存限制，防止单个容器吃光 WSL 内存
ENV JAVA_OPTS="-Xms256m -Xmx512m"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]