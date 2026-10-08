FROM eclipse-temurin:11-jre-jammy
LABEL org.opencontainers.image.title="US Course"
ENV TZ=Asia/Shanghai JAVA_OPTS="-Xms128m -Xmx384m"
WORKDIR /app
COPY app.jar /app/app.jar
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar \"$@\"", "--"]
