# INBusiness Phase 2 - Production Release Checklist

## 🎯 Pre-Release Verification

### Code Quality ✅
- [ ] All build warnings resolved
- [ ] No TODO/FIXME comments in production code
- [ ] All hardcoded values moved to constants
- [ ] No debug logs in release code (ProGuard removes them)
- [ ] All placeholder UI text replaced with proper strings

### Dependencies ✅
- [ ] All Phase 2 dependencies added to build.gradle
- [ ] BouncyCastle version 1.70+
- [ ] Apache Commons Math 3.6.1
- [ ] Retrofit 2.9.0+
- [ ] Room 2.6.1+
- [ ] Hilt 2.51+

### Security ✅
- [ ] SQLCipher encryption enabled
- [ ] BouncyCastle provider registered in Application class
- [ ] NIC credentials in local.properties (NOT in Git)
- [ ] Android Keystore integration tested
- [ ] No sensitive data in logs (release build)
- [ ] FileProvider paths secure (only invoices/ and pdfs/)

### NIC E-Invoicing Integration ✅
- [ ] NIC sandbox credentials working
- [ ] Authentication flow tested
- [ ] IRN generation successful
- [ ] QR code embedded in PDF
- [ ] Error handling for all NIC APIs tested
- [ ] Offline mode gracefully handles no network
- [ ] Circuit breaker prevents cascade failures

### AI Anomaly Detection ✅
- [ ] 5 anomaly types implemented
- [ ] Z-score threshold configured (2.5)
- [ ] GST ratio validation working
- [ ] Duplicate detection functional
- [ ] Timing anomaly detection tested
- [ ] Warning UI displays correctly

### Database ✅
- [ ] Migration from v1 to v2 tested
- [ ] Invoice table has irn, ackNo, qrCodeData fields
- [ ] Encryption passphrase secure
- [ ] Backup rules configured
- [ ] Data extraction rules configured

### Performance ✅
- [ ] Baseline Profile generated
- [ ] Cold start time < 1.5 seconds
- [ ] Invoice list loads < 500ms (100 items)
- [ ] PDF generation < 3 seconds
- [ ] IRN generation < 5 seconds (with network)
- [ ] No memory leaks (verified with Android Profiler)
- [ ] No ANRs during testing

### Features Testing ✅
- [ ] Calculator: GST calculations correct
- [ ] Dashboard: Metrics update in real-time
- [ ] Create Invoice: All fields save correctly
- [ ] Edit Invoice: Changes persist
- [ ] PDF Generation: QR code visible
- [ ] IRN Generation: Sandbox API working
- [ ] Anomaly Warnings: Display for outliers
- [ ] Offline Mode: Create invoices without network
- [ ] Data Sync: Resume IRN generation when online

### UI/UX ✅
- [ ] Dark mode works correctly
- [ ] All screens responsive on phones (5" - 7")
- [ ] Material3 theme applied consistently
- [ ] Loading states show during operations
- [ ] Error messages user-friendly
- [ ] Success feedback clear
- [ ] Navigation smooth (no jank)

### Testing Coverage ✅
- [ ] Tested on Android 10 (API 29)
- [ ] Tested on Android 12 (API 31)
- [ ] Tested on Android 14 (API 34)
- [ ] Tested on Android 15 (API 35)
- [ ] Tested on low-end device (< 2GB RAM)
- [ ] Tested on high-end device (flagship)
- [ ] Tested with 1000+ invoices in database
- [ ] Tested with slow network (2G simulation)

### Build Configuration ✅
- [ ] ProGuard rules complete
- [ ] Signing config setup
- [ ] Version code incremented to 2
- [ ] Version name set to 2.0.0
- [ ] Target SDK 35 (latest Play Store requirement)
- [ ] Min SDK 26 (96% device coverage)

### Play Store Requirements ✅
- [ ] Privacy policy URL added
- [ ] Data safety form completed
- [ ] App screenshots ready (2-8 images)
- [ ] Feature graphic created (1024x500)
- [ ] Short description written (<80 chars)
- [ ] Full description written
- [ ] Content rating submitted
- [ ] Target audience set
- [ ] Store listing category selected

### Documentation ✅
- [ ] README.md updated with Phase 2 features
- [ ] CHANGELOG.md created
- [ ] API documentation for NIC integration
- [ ] User guide for e-invoicing
- [ ] Developer notes for future maintainers

---

## 🚀 Build Steps

### 1. Pre-Build
```
# Verify configuration
./gradlew :app:dependencies
./gradlew :app:hiltAggregate

# Run lint
./gradlew :app:lintRelease
```

### 2. Generate Baseline Profile (Optional - First Release)
```
./gradlew :baselineprofile:pixel6Api35Setup
./gradlew :baselineprofile:generateBaselineProfile
```

### 3. Build Release
```
./build-release.sh
# Or manually:
./gradlew clean
./gradlew :app:assembleRelease
./gradlew :app:bundleRelease
```

### 4. Verify Outputs
```
# APK
ls -lh app/build/outputs/apk/release/app-release.apk

# AAB
ls -lh app/build/outputs/bundle/release/app-release.aab
```

---

## 📦 Deployment Checklist

### Internal Testing (1-3 days)
- [ ] Install APK on test devices
- [ ] Verify all features work
- [ ] Test NIC sandbox integration
- [ ] Test offline mode
- [ ] Monitor crash reports

### Closed Alpha Testing (3-7 days)
- [ ] Upload AAB to Play Console
- [ ] Add 20-50 alpha testers
- [ ] Collect feedback
- [ ] Monitor metrics (crashes, ANRs)
- [ ] Fix critical issues

### Open Beta Testing (7-14 days)
- [ ] Expand to 100+ testers
- [ ] Monitor crash-free rate (target: >99%)
- [ ] Monitor ANR rate (target: <0.5%)
- [ ] Collect user feedback
- [ ] Fix remaining bugs

### Production Rollout
- [ ] Staged rollout: 1% → 5% → 10% → 25% → 50% → 100%
- [ ] Monitor at each stage
- [ ] Pause if crash rate > 1%
- [ ] Update NIC credentials to production
- [ ] Monitor Play Console Vitals

---

## 🎯 Success Metrics

### Performance
- Cold start: < 1.5 seconds ✅
- Warm start: < 500ms ✅
- Frame rate: 60 FPS (no jank) ✅

### Stability
- Crash-free rate: >99.5% ✅
- ANR rate: <0.5% ✅

### Size
- APK size: <8 MB ✅
- AAB size: <6 MB ✅

### User Retention
- Day 1: >40%
- Day 7: >20%
- Day 30: >10%

---

## 🔄 Post-Release

### Immediate (Day 1-3)
- [ ] Monitor crash reports
- [ ] Monitor ANR reports
- [ ] Check Play Console Vitals
- [ ] Respond to user reviews
- [ ] Track adoption rate

### Short-term (Week 1-2)
- [ ] Analyze user feedback
- [ ] Plan hotfix if needed
- [ ] Monitor NIC API usage
- [ ] Track IRN generation success rate

### Long-term (Month 1)
- [ ] Analyze retention metrics
- [ ] Plan Phase 3 features
- [ ] Optimize based on usage patterns

---

## ⚠️ Emergency Rollback Plan

If critical issues detected:

1. **Immediate Action**
   - Halt rollout in Play Console
   - Disable NIC integration (feature flag)

2. **Hotfix**
   - Fix critical bug
   - Test thoroughly
   - Deploy patch ASAP

3. **Communication**
   - Notify users via Play Store listing
   - Respond to reviews
   - Post update timeline

---

## 📞 Support Contacts

- **NIC Technical Support**: support@nic.in
- **Play Store Support**: https://support.google.com/googleplay/android-developer
- **Emergency Contact**: [Your contact]

---

**Last Updated**: November 20, 2025
**Version**: 2.0.0
**Build**: 2
```