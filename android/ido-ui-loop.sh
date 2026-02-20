#!/bin/bash

# iDo UI Feedback Loop Script
# This script facilitates iterative UI refinement through a terminal-based feedback loop.

set -e

APP_PACKAGE="com.ido.app"
MAIN_ACTIVITY="com.ido.app.MainActivity"

echo "╔════════════════════════════════════════════════════════════╗"
echo "║           iDo UI Feedback Loop - v1.0                     ║"
echo "╠════════════════════════════════════════════════════════════╣"
echo "║  Commands:                                                 ║"
echo "║    continue   - Polish current implementation             ║"
echo "║    fix: <msg> - Apply specific fix based on feedback      ║"
echo "║    finalize   - Exit the feedback loop                    ║"
echo "╚════════════════════════════════════════════════════════════╝"
echo ""

# Function to build and install the app
build_and_install() {
    echo "📦 Building and installing debug APK..."
    ./gradlew installDebug --quiet
    
    if [ $? -eq 0 ]; then
        echo "✅ Build successful!"
    else
        echo "❌ Build failed!"
        return 1
    fi
}

# Function to launch the app
launch_app() {
    echo "🚀 Launching iDo app..."
    adb shell am start -n "${APP_PACKAGE}/${MAIN_ACTIVITY}" 2>/dev/null || {
        echo "⚠️  Could not launch app automatically. Please open it manually."
    }
}

# Function to show feedback prompt
show_prompt() {
    echo ""
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo "📱 Review the app on your device"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo ""
}

# Initial build and launch
build_and_install
launch_app
show_prompt

# Main feedback loop
while true; do
    echo -n "feedback> "
    read -r input
    
    if [ -z "$input" ]; then
        continue
    fi
    
    case "$input" in
        "continue"|"c")
            echo ""
            echo "🔄 Polishing current implementation..."
            echo "   (Make changes in code, then run 'continue' or 'fix: <feedback>' again)"
            build_and_install
            launch_app
            show_prompt
            ;;
        fix:*)
            feedback="${input#fix:}"
            feedback="${feedback#"${feedback%%[![:space:]]*}"}" # trim leading whitespace
            echo ""
            echo "🛠️  Applying fix based on feedback:"
            echo "   \"$feedback\""
            echo ""
            echo "   Please make the necessary code changes, then press Enter to rebuild."
            read -r
            build_and_install
            launch_app
            show_prompt
            ;;
        "finalize"|"done"|"exit"|"quit"|"q")
            echo ""
            echo "✨ Finalizing UI revamp..."
            echo "   Thank you for using iDo UI Feedback Loop!"
            echo ""
            exit 0
            ;;
        "rebuild"|"r")
            echo ""
            echo "🔨 Rebuilding..."
            build_and_install
            launch_app
            show_prompt
            ;;
        "help"|"h"|"?")
            echo ""
            echo "Available commands:"
            echo "  continue (c)    - Rebuild and continue polishing"
            echo "  fix: <message>  - Log feedback, then rebuild after you make changes"
            echo "  rebuild (r)     - Just rebuild without any feedback"
            echo "  finalize (q)    - Exit the feedback loop"
            echo "  help (h)        - Show this help message"
            echo ""
            ;;
        *)
            echo "Unknown command: $input"
            echo "Type 'help' for available commands."
            ;;
    esac
done
