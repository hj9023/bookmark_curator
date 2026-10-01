# 1단계: 빌드 스테이지 (Gradle 빌드)
FROM eclipse-temurin:17-jdk AS builder
WORKDIR /workspace

# Gradle 캐시 효율화를 위해 래퍼 및 설정 파일 먼저 복사
COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./

# gradlew 실행 권한 부여
RUN chmod +x ./gradlew

# 소스 코드 복사
COPY src src

# 테스트 제외하고 JAR 빌드 및 실행 JAR 분리
RUN ./gradlew clean bootJar -x test --no-daemon && \
    cp $(ls build/libs/*.jar | grep -v 'plain') app.jar

# 2단계: 실행 스테이지 (JRE 경량 이미지)
FROM eclipse-temurin:17-jre
WORKDIR /app

# 빌드 스테이지에서 생성된 JAR 복사
COPY --from=builder /workspace/app.jar app.jar

# 포트 노출
EXPOSE 8080

# JAR 실행
ENTRYPOINT ["java", "-jar", "app.jar"]
