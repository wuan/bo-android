🟡 Medium Effort (Days to Weeks)

8. Replace HttpURLConnection with Retrofit/OkHttp

Location: jsonrpc/HttpServiceClientDefault.kt
- Modernizes networking layer
- Better error handling, interceptors, logging
  Impact: Cleaner code, better performance

9. Add ViewModel Layer

Create: app/viewmodels/MainViewModel.kt
- Add ViewModel for Main activity
- Separates UI logic from business logic
  Files affected: app/Main.kt (refactor 100+ lines)

10. Increase Test Coverage

- Add tests for AppService.kt (currently untested)
- Add UI tests for major screens
- Target: 40-50% coverage (currently ~20%)
  Effort: Write ~20-30 new tests

11. Implement LRU Cache with Size Limits

Location: data/cache/DataCache.kt
private val cache = object : LinkedHashMap<Parameters, ResultEvent>(
initialCapacity = 16,
loadFactor = 0.75f,
accessOrder = true
) {
override fun removeEldestEntry(eldest: Map.Entry<*, *>) = size > 50
}
Impact: Prevents memory leaks

12. Fix Fragment Lifecycle Deprecation

Location: map/MapFragment.kt:49
- Move logic from onActivityCreated() to onViewCreated()

  ---
🟠 High Effort (Weeks to Months)

13. Migrate KAPT to KSP

Impact: 25-50% faster build times
Requires:
- Update to Hilt (or wait for Dagger KSP support)
- Update app/build.gradle.kts
- Test all DI thoroughly

14. Replace Handler-based Threading with Coroutines

Locations:
- data/MainDataHandler.kt:354-386
- dagger/module/ServiceModule.kt
- All Handler.post() calls throughout codebase

Before:
handler.postDelayed({ updateData() }, interval)

After:
viewModelScope.launch {
while(isActive) {
updateData()
delay(interval)
}
}
Impact: Modern, cancellable, easier to test

15. Migrate Dagger 2 to Hilt

Affected: All files in dagger/ package
- Reduces boilerplate by ~50%
- Better AndroidX integration
- Enables KSP migration
  Effort: 2-3 weeks (all DI needs rewriting)

16. Replace ConsumerContainer with StateFlow

Location: protocol/ConsumerContainer.kt
Affected: 15+ files using ConsumerContainer
// Replace
val dataConsumerContainer = object : ConsumerContainer<DataEvent>() {}

// With
private val _dataEvents = MutableStateFlow<DataEvent?>(null)
val dataEvents: StateFlow<DataEvent?> = _dataEvents.asStateFlow()
Impact: Better lifecycle awareness, built-in backpressure

17. Add Repository Layer

Create: data/repository/ package
- Abstracts data sources
- Enables offline-first architecture
- Easier testing with fakes
  Files to refactor: MainDataHandler.kt, ServiceDataHandler.kt

  ---
🔴 Very High Effort (Months)

18. Modularize Codebase

Structure:
:app
:core
:feature-main
:feature-alerts
:feature-settings
:data
Impact: Better build times, clearer boundaries, parallel development
Effort: 2-3 months

19. Add Jetpack Compose for New UI

- Incrementally migrate screens to Compose
- Keep existing views for now
- Start with simple screens (Settings, Dialogs)
  Effort: 3-6 months for full migration

20. Implement Offline-First Architecture with Room

- Add Room database for persistent caching
- Implement proper repository pattern
- Add WorkManager for background sync
  Effort: 1-2 months

21. Refactor Main.kt (694 lines)

Split into:
- MainViewModel (state management)
- MainCoordinator (navigation)
- Multiple smaller components
- Extract overlays to separate classes
  Effort: 3-4 weeks of careful refactoring

  ---
📊 Summary by Impact vs Effort

High Impact, Low Effort ⭐ (Do First)

1. Enable ProGuard/R8
2. Add Gradle optimizations
3. Fix deprecated Handler
4. Add static analysis (detekt)

High Impact, Medium Effort 🎯

6. Migrate Activity Result API
7. Add ViewModel layer
8. Replace HttpURLConnection with Retrofit
9. Increase test coverage to 40%+

High Impact, High Effort 🚀 (Strategic)

14. Migrate to Coroutines (replace Handler)
15. Migrate to Hilt
16. Replace ConsumerContainer with StateFlow
17. Add Repository layer

Lower Priority 📋

11. LRU cache implementation
12. Full modularization
13. Jetpack Compose migration
14. Offline-first with Room

  ---
Recommended Roadmap:
1. Sprint 1: Items #1-5 (quick wins)
2. Sprint 2-3: Items #6-9 (deprecation fixes + ViewModel)
3. Sprint 4-6: Items #14, #16 (Coroutines migration)
4. Quarter 2: Items #15, #17 (Hilt + Repository)
5. Long-term: Items #18-21 (major refactors)
