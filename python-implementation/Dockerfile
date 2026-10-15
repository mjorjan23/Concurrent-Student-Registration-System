FROM python:3.12-slim

# Set the working directory inside the container
WORKDIR /app

# Copy the project into the container
COPY . .

# Make the async run script executable
RUN chmod +x run_python_async.sh

# Default command (Assignment 4)
CMD ["./run_python_async.sh"]