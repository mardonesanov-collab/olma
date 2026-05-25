# ============================================================
# Stage 1: Build Frontend
# ============================================================
FROM node:18-alpine AS frontend-build

WORKDIR /app/menubot/frontend

# Copy frontend package files
COPY menubot/frontend/package.json menubot/frontend/package-lock.json ./

# Install dependencies
RUN npm ci

# Copy frontend source
COPY menubot/frontend/ ./

# Build the frontend with output to dist directory
RUN npm run build -- --outDir /app/frontend-dist

# ============================================================
# Stage 2: Build Spring Boot Application
# ============================================================
FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /app

# Copy pom.xml and download dependencies (cached layer)
COPY menubot/pom.xml ./
RUN mvn dependency:go-offline -B

# Copy source code
COPY menubot/src ./src

# Copy the built frontend to static resources
COPY --from=frontend-build /app/frontend-dist ./src/main/resources/static

# Build the application
RUN mvn package -DskipTests -B

# ============================================================
# Stage 3: Runtime
# ============================================================
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Create directory for uploads
RUN mkdir -p /app/uploads

# Copy the built JAR file
COPY --from=build /app/target/*.jar app.jar

# Expose port
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "/app/app.jar"]