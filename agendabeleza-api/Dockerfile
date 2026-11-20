# syntax=docker/dockerfile:1

# Contexto de build: raiz do workspace (pasta pai de agendabeleza-api).
# Exemplo: docker build -f agendabeleza-api/Dockerfile --target production .

FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /workspace/agendabeleza-api

COPY ./agendabeleza-api/mvnw ./mvnw
COPY ./agendabeleza-api/.mvn ./.mvn
COPY ./agendabeleza-api/pom.xml ./pom.xml

RUN sed -i 's/\r$//' mvnw && chmod +x mvnw

RUN --mount=type=cache,target=/root/.m2 \
	./mvnw -B dependency:go-offline

COPY ./agendabeleza-api/src ./src

RUN --mount=type=cache,target=/root/.m2 \
	./mvnw -B package -DskipTests && \
	cp target/agendabeleza-api.jar /workspace/app.jar

FROM eclipse-temurin:21-jre-alpine AS runtime-base

RUN apk add --no-cache \
	fontconfig \
	ttf-dejavu \
	wget \
	tzdata

RUN cp /usr/share/zoneinfo/America/Sao_Paulo /etc/localtime && \
	echo "America/Sao_Paulo" > /etc/timezone

RUN addgroup -g 1001 -S appgroup && \
	adduser -u 1001 -S appuser -G appgroup

WORKDIR /app

ENV USER_TIMEZONE="America/Sao_Paulo" \
	TZ=America/Sao_Paulo \
	LANG=pt_BR.UTF-8 \
	LANGUAGE=pt_BR:pt:en \
	LC_ALL=pt_BR.UTF-8 \
	LC_TIME=pt_BR.UTF-8 \
	LC_MONETARY=pt_BR.UTF-8 \
	SPRING_OUTPUT_ANSI_ENABLED=ALWAYS

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 \
	-XX:+UseG1GC \
	-XX:+UseStringDeduplication \
	-Dspring.backgroundpreinitializer.ignore=true \
	-Dfile.encoding=UTF-8 \
	-Duser.timezone=America/Sao_Paulo"

COPY --from=build --chown=appuser:appgroup /workspace/app.jar /app/app.jar

USER appuser

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
	CMD wget --quiet --tries=1 --spider http://localhost:8080/actuator/health || exit 1

CMD ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]

FROM runtime-base AS development

ENV SPRING_PROFILES_ACTIVE=dev \
	JAVA_OPTS="${JAVA_OPTS} \
	-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:8000"

EXPOSE 8000

FROM runtime-base AS production

ENV SPRING_PROFILES_ACTIVE=production
