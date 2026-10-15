FROM eclipse-temurin:17

# Set the working directory inside the container
WORKDIR /app

# Copy the project into the container
COPY . .

# Make the run script executable
RUN chmod +x run_java_async.sh

# Default command (Assignment 4)
CMD ["./run_java_async.sh"]