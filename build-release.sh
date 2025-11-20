#!/bin/bash

# ═══════════════════════════════════════════════════════════════════════════
# INBusiness Phase 2 - Production Release Build Script
# Generates: APK + AAB with Baseline Profile
# ═══════════════════════════════════════════════════════════════════════════

set -e  # Exit on error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
echo -e "${BLUE}  INBusiness Phase 2 - Release Build Script${NC}"
echo -e "${BLUE}  E-Invoicing | AI Anomaly | Baseline Profile${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
echo ""

# ═══════════════════════════════════════════════════════════════════════════
# Pre-Flight Checks
# ═══════════════════════════════════════════════════════════════════════════
echo -e "${CYAN}📋 Running pre-flight checks...${NC}"

# Check if local.properties exists
if [ ! -f "local.properties" ]; then
    echo -e "${RED}❌ Error: local.properties not found${NC}"
    echo -e "${YELLOW}Create local.properties with NIC credentials:${NC}"
    echo ""
    echo "NIC_USERNAME=your_username"
    echo "NIC_PASSWORD=your_password"
    echo "NIC_BASE_URL=https://einv-apisandbox.nic.in"
    echo "ENABLE_NIC=true"
    echo ""
    exit 1
fi

# Check for NIC credentials
if ! grep -q "NIC_USERNAME" local.properties; then
    echo -e "${YELLOW}⚠️  Warning: NIC credentials not found in local.properties${NC}"
    read -p "Continue anyway? (y/n) " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        exit 1
    fi
fi

# Check for signing config
if [ ! -f "keystore.jks" ] && [ ! -f "../keystore.jks" ]; then
    echo -e "${YELLOW}⚠️  Warning: keystore.jks not found. Build will be unsigned.${NC}"
fi

echo -e "${GREEN}✓ Pre-flight checks passed${NC}"
echo ""

# ═══════════════════════════════════════════════════════════════════════════
# Clean Build
# ═══════════════════════════════════════════════════════════════════════════
echo -e "${CYAN}🧹 Cleaning previous builds...${NC}"
./gradlew clean

if [ $? -ne 0 ]; then
    echo -e "${RED}❌ Clean failed${NC}"
    exit 1
fi

echo -e "${GREEN}✓ Clean complete${NC}"
echo ""

# ═══════════════════════════════════════════════════════════════════════════
# Baseline Profile Generation (Optional)
# ═══════════════════════════════════════════════════════════════════════════
read -p "Generate Baseline Profile? (Recommended for production) (y/n) " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo -e "${CYAN}📊 Generating Baseline Profile (this may take 5-10 minutes)...${NC}"

    # Setup benchmark device
    ./gradlew :baselineprofile:pixel6Api35Setup

    if [ $? -ne 0 ]; then
        echo -e "${YELLOW}⚠️  Benchmark setup failed. Continuing without baseline profile.${NC}"
    else
        # Generate profile
        ./gradlew :baselineprofile:generateBaselineProfile

        if [ $? -eq 0 ]; then
            echo -e "${GREEN}✓ Baseline Profile generated successfully${NC}"
        else
            echo -e "${YELLOW}⚠️  Baseline Profile generation failed. Continuing without it.${NC}"
        fi
    fi
    echo ""
fi

# ═══════════════════════════════════════════════════════════════════════════
# Lint Checks
# ═══════════════════════════════════════════════════════════════════════════
echo -e "${CYAN}🔍 Running lint checks...${NC}"
./gradlew :app:lintRelease

if [ $? -ne 0 ]; then
    echo -e "${YELLOW}⚠️  Lint checks failed. Review app/build/reports/lint-results-release.html${NC}"
    read -p "Continue anyway? (y/n) " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        exit 1
    fi
fi

echo -e "${GREEN}✓ Lint checks complete${NC}"
echo ""

# ═══════════════════════════════════════════════════════════════════════════
# Build Release APK
# ═══════════════════════════════════════════════════════════════════════════
echo -e "${CYAN}🔨 Building release APK...${NC}"
./gradlew :app:assembleRelease

if [ $? -ne 0 ]; then
    echo -e "${RED}❌ APK build failed${NC}"
    exit 1
fi

APK_PATH="app/build/outputs/apk/release/app-release.apk"

if [ -f "$APK_PATH" ]; then
    APK_SIZE=$(du -h "$APK_PATH" | cut -f1)
    echo -e "${GREEN}✅ Release APK built successfully${NC}"
    echo -e "   ${BLUE}Size:${NC} $APK_SIZE"
    echo -e "   ${BLUE}Location:${NC} $APK_PATH"
else
    echo -e "${RED}❌ APK not found at expected location${NC}"
    exit 1
fi

echo ""

# ═══════════════════════════════════════════════════════════════════════════
# Build Release AAB (App Bundle for Play Store)
# ═══════════════════════════════════════════════════════════════════════════
echo -e "${CYAN}📦 Building release AAB...${NC}"
./gradlew :app:bundleRelease

if [ $? -ne 0 ]; then
    echo -e "${RED}❌ AAB build failed${NC}"
    exit 1
fi

AAB_PATH="app/build/outputs/bundle/release/app-release.aab"

if [ -f "$AAB_PATH" ]; then
    AAB_SIZE=$(du -h "$AAB_PATH" | cut -f1)
    echo -e "${GREEN}✅ Release AAB built successfully${NC}"
    echo -e "   ${BLUE}Size:${NC} $AAB_SIZE"
    echo -e "   ${BLUE}Location:${NC} $AAB_PATH"
else
    echo -e "${RED}❌ AAB not found at expected location${NC}"
    exit 1
fi

echo ""

# ═══════════════════════════════════════════════════════════════════════════
# Build Summary
# ═══════════════════════════════════════════════════════════════════════════
echo -e "${GREEN}═══════════════════════════════════════════════════════════════${NC}"
echo -e "${GREEN}  ✅ BUILD SUCCESSFUL!${NC}"
echo -e "${GREEN}═══════════════════════════════════════════════════════════════${NC}"
echo ""
echo -e "${BLUE}📱 APK:${NC} $APK_SIZE at $APK_PATH"
echo -e "${BLUE}📦 AAB:${NC} $AAB_SIZE at $AAB_PATH"
echo ""
echo -e "${CYAN}📝 Next Steps:${NC}"
echo "  1. Test APK on physical devices (Android 10, 12, 14, 15)"
echo "  2. Verify NIC IRN generation with sandbox credentials"
echo "  3. Test AI anomaly detection with sample invoices"
echo "  4. Upload AAB to Play Console Internal Testing"
echo "  5. Get production NIC credentials from NIC portal"
echo ""
echo -e "${YELLOW}⚠️  Remember to:${NC}"
echo "  • Update local.properties with production NIC URL"
echo "  • Test on low-end devices (2GB RAM)"
echo "  • Monitor crash reports in Play Console"
echo ""
echo -e "${GREEN}═══════════════════════════════════════════════════════════════${NC}"