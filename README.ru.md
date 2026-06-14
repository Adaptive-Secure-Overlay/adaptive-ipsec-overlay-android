# Adaptive Secure Overlay IPsec Android

[![English](https://img.shields.io/badge/language-English-0f172a?style=flat-square)](README.md)
[![Русская версия](https://img.shields.io/badge/language-Русская%20версия-0369a1?style=flat-square)](README.ru.md)

Android-ветка Adaptive Secure Overlay IPsec — это клиентский прототип, который показывает логику overlay и маршрутной сессии на мобильной платформе, но пока не претендует на готовый Android VPN-клиент.

## Текущий этап

- APK собирается
- базовый Android UI уже есть
- логика выбора A/B и режима Random или Manual X1/X2 уже есть
- состояние сессии и исследовательский лог уже моделируются в приложении
- запущен отдельный Rust `native-core` для будущего crypto/session слоя
- добавлен первый JNI-контракт между Kotlin и Rust
- `.so` библиотеки Rust уже собираются и пакуются в APK для `arm64-v8a`, `armeabi-v7a` и `x86_64`
- реальный transport, реальная криптография и реальная установка VPN/IPsec на Android пока не подключены

## Что уже достигнуто

- сформирован базовый Android-клиентский интерфейс для IPsec-трека
- сохранена логика той же overlay-модели, что используется в лабораторной схеме
- подготовлена точка входа для будущего service/VpnService слоя
- создан отдельный Rust crate под Android crypto/core
- добавлен первый мост Kotlin -> Rust через JNI
- подготовлен packaging-путь для будущей `.so` библиотеки через `jniLibs`
- сборка и упаковка реальных Android `.so` уже работает в проекте
- сборка APK уже позволяет вести отдельный Android-трек публично

## Что пока не готово

- live control-plane transport
- проверка live-загрузки native-библиотеки и self-test на устройстве/эмуляторе
- реальный X25519/HKDF обмен на Android через этот слой
- реальная установка IKE/ESP или эквивалентного Android data-plane
- интеграция с `VpnService` или внешним IPsec backend

## Native core

- `native-core/` — это первый Rust слой для Android crypto/core.
- Сейчас в нем уже есть модель bootstrap-сессии, X25519, HKDF-SHA256 и JNI exports.
- Kotlin-сторона уже готова к вызову, а `libadaptive_overlay_core.so` уже пакуется в APK через `jniLibs`.
- `build-native-core.ps1` уже подготавливает packaging-слой и станет точкой сборки `.so` на следующем этапе.

## Сборка

Из корня репозитория:

```powershell
.\build-android-apk.ps1
```

## Статус

Публичный клиентский прототип. Это честная Android-ветка проекта, но пока еще не готовый мобильный endpoint в полном смысле.
