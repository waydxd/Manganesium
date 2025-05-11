#!/bin/bash

# Function to clean up background processes
cleanup() {
    echo "Terminating backend and frontend processes..."
    # Kill the backend and frontend processes if they exist
    if [ ! -z "$BACKEND_PID" ]; then
        kill $BACKEND_PID 2>/dev/null
    fi
    if [ ! -z "$FRONTEND_PID" ]; then
        kill $FRONTEND_PID 2>/dev/null
    fi
    # Ensure any remaining gradle or npm processes are terminated
    pkill -f "gradle" 2>/dev/null
    pkill -f "node" 2>/dev/null
    echo "All processes terminated."
    exit 0
}

# Trap Ctrl+C (SIGINT) and call cleanup
trap cleanup SIGINT

# Start the backend
echo "Starting backend..."
./gradlew run &
BACKEND_PID=$!

# Wait a few seconds to ensure backend starts
sleep 5

# Navigate to the web directory and start the frontend
echo "Starting frontend..."
cd web
npm run build
npm run preview &
FRONTEND_PID=$!

# Wait for both processes to complete
wait $BACKEND_PID $FRONTEND_PID