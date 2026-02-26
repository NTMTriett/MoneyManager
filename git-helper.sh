#!/bin/bash
# Git Helper Scripts for MoneyManager Development

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to display status
show_status() {
    echo -e "${BLUE}=== Git Status ===${NC}"
    git status
    echo ""
    echo -e "${BLUE}=== Recent Commits ===${NC}"
    git log --oneline -5
}

# Function to create feature branch
create_feature() {
    if [ -z "$1" ]; then
        echo -e "${RED}Error: Please provide a feature name${NC}"
        echo "Usage: create_feature feature-name"
        return 1
    fi
    
    echo -e "${GREEN}Creating feature branch: feature/$1${NC}"
    git checkout -b "feature/$1"
}

# Function to create fix branch
create_fix() {
    if [ -z "$1" ]; then
        echo -e "${RED}Error: Please provide a fix name${NC}"
        echo "Usage: create_fix fix-name"
        return 1
    fi
    
    echo -e "${GREEN}Creating fix branch: fix/$1${NC}"
    git checkout -b "fix/$1"
}

# Function to quick commit
quick_commit() {
    if [ -z "$1" ]; then
        echo -e "${RED}Error: Please provide a commit message${NC}"
        echo "Usage: quick_commit \"Your commit message\""
        return 1
    fi
    
    echo -e "${GREEN}Adding all changes and committing...${NC}"
    git add .
    git commit -m "$1"
}

# Function to merge and cleanup feature
finish_feature() {
    current_branch=$(git branch --show-current)
    
    if [[ $current_branch != feature/* ]]; then
        echo -e "${RED}Error: Not on a feature branch${NC}"
        return 1
    fi
    
    echo -e "${YELLOW}Switching to master and merging $current_branch${NC}"
    git checkout master
    git merge "$current_branch"
    
    echo -e "${YELLOW}Deleting feature branch: $current_branch${NC}"
    git branch -d "$current_branch"
    
    echo -e "${GREEN}Feature merged and cleaned up!${NC}"
}

# Function to show help
show_help() {
    echo -e "${BLUE}MoneyManager Git Helper${NC}"
    echo ""
    echo "Available commands:"
    echo "  status              - Show git status and recent commits"
    echo "  feature <name>      - Create new feature branch"
    echo "  fix <name>          - Create new fix branch"
    echo "  commit \"message\"    - Quick add and commit all changes"
    echo "  finish              - Merge current feature branch to master and cleanup"
    echo "  help                - Show this help"
    echo ""
    echo "Examples:"
    echo "  ./git-helper.sh feature add-budget-tracking"
    echo "  ./git-helper.sh commit \"Add transaction validation\""
    echo "  ./git-helper.sh finish"
}

# Main script logic
case "$1" in
    "status")
        show_status
        ;;
    "feature")
        create_feature "$2"
        ;;
    "fix")
        create_fix "$2"
        ;;
    "commit")
        quick_commit "$2"
        ;;
    "finish")
        finish_feature
        ;;
    "help"|"")
        show_help
        ;;
    *)
        echo -e "${RED}Unknown command: $1${NC}"
        show_help
        ;;
esac