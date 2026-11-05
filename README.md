# ⚽ Halı Saha Rezervasyon & Takım Bulma Platformu

Halı saha rezervasyonu yapmak, eksik oyuncu bulmak ve rakip takım bulmak için geliştirilmiş bir Spring Boot web uygulaması. REST API ile birlikte düz HTML/CSS/JS arayüzünü tek bir uygulama olarak sunar.

**🌐 Canlı demo:** https://halisaha-api-0cua.onrender.com/login.html
**📘 API dokümantasyonu (Swagger):** https://halisaha-api-0cua.onrender.com/swagger-ui.html

> Uygulama Render'ın ücretsiz planında çalışıyor. 15 dakika kullanılmazsa uyur, ilk açılış 30–60 saniye sürebilir.

## Özellikler

- **Kayıt / giriş:** BCrypt ile parola hashleme, JWT tabanlı kimlik doğrulama.
- **Saha rezervasyonu:** 17:00–02:00 arası, 1 saatlik slotlar. Dolu veya geçmiş saatler listelenmez, geçmiş saate rezervasyon yapılamaz.
- **Çift rezervasyon koruması:** Pessimistic locking ve çakışma sorgusu. Aynı saate gelen eşzamanlı isteklerden yalnızca biri başarılı olur.
- **Eksik oyuncu ilanı:** Rezervasyon sahibi ilan açar, diğer kullanıcılar başvurur, ilan sahibi başvuruları kabul veya reddeder. Bekleyen başvuru sayısı ilanda görünür.
- **Rakip bulma:** Takım kaptanı meydan okuma ilanı açar. Başka bir takımın kaptanı kabul edince maç kaydı otomatik oluşur.
- **Takımlar:** Takım oluşturma ve takımlarını listeleme.
- **Maçlarım:** Gelecek ve geçmiş maçlar ayrı sekmelerde. Geçmiş maçlarda ilan işlemleri kapalıdır.
- **Giriş denemesi sınırı:** E-posta başına 15 dakikada 5 başarısız denemeden sonra HTTP 429 döner.
- **Hata yönetimi:** Global exception handler ile anlamlı HTTP hata kodları ve mesajları.

## Teknolojiler

| Katman | Teknoloji |
|---|---|
| Dil / Çatı | Java 21, Spring Boot 4 |
| Güvenlik | Spring Security, JWT, BCrypt |
| Veri | Spring Data JPA / Hibernate, PostgreSQL |
| Dokümantasyon | springdoc-openapi (Swagger UI) |
| Test | JUnit 5, Mockito, Spring Boot Test |
| Arayüz | HTML, CSS, JavaScript (Spring Boot içinden servis edilir) |
| Dağıtım | Docker (çok aşamalı build), Render, Neon PostgreSQL |

## Mimari

Katmanlı monolit: tek bir uygulama içinde sorumluluklar paketlere ayrılmıştır. Mikroservis değildir.

```
src/main/java/com/halisaha/api/
├── controller/   REST uç noktaları
├── service/      iş kuralları (rezervasyon, müsaitlik, ilan, başvuru, maç)
├── repository/   Spring Data JPA arayüzleri
├── entity/       JPA varlıkları (User, Venue, Reservation, Team, OpenMatch, JoinRequest, OpenChallenge, Match)
├── dto/          istek / yanıt modelleri
├── exception/    özel hatalar ve GlobalExceptionHandler
├── security/     JWT filtresi, JwtService, LoginRateLimiter
└── config/       SecurityConfig, PasswordConfig, DataSeeder
src/main/resources/static/   HTML, CSS ve JS arayüzü
```

## API özeti

Kayıt ve giriş dışındaki tüm uç noktalar `Authorization: Bearer <token>` başlığı ister. Ayrıntılar Swagger'da.

| Metot | Yol | Açıklama |
|---|---|---|
| POST | `/api/users/register`, `/api/users/login` | Kayıt ve giriş (açık) |
| GET | `/api/users/me` | Giriş yapmış kullanıcı |
| GET | `/api/venues` | Sahalar |
| GET | `/api/venues/{id}/available-slots?date=` | Müsait saatler |
| POST | `/api/reservations` | Rezervasyon oluştur |
| GET | `/api/reservations/me` | Rezervasyonlarım |
| POST / GET | `/api/open-matches` | Eksik oyuncu ilanı aç / listele |
| GET | `/api/open-matches/{id}/join-requests` | İlana gelen başvurular |
| POST | `/api/join-requests` | İlana başvur |
| GET | `/api/join-requests/me` | Başvurularım |
| POST | `/api/join-requests/{id}/accept`, `/reject` | Başvuruyu kabul / reddet |
| POST / GET | `/api/open-challenges` | Rakip bulma ilanı aç / listele |
| POST | `/api/open-challenges/{id}/accept` | Meydan okumayı kabul et |
| POST / GET | `/api/teams`, `/api/teams/me` | Takım oluştur / takımlarım |
| GET | `/api/matches/me` | Maçlarım |

## Yerelde çalıştırma

**Gereksinimler:** JDK 21 ve PostgreSQL. Maven için projedeki `mvnw` yeterli.

1. Veritabanı oluştur (varsayılan ad `halisaha`, kullanıcı `postgres`, `localhost:5432`):
   ```sql
   CREATE DATABASE halisaha;
   ```
2. Gizli bilgiler için yerel yapılandırma dosyasını oluştur. `config/application.properties` git'e girmez, Spring Boot bu dosyayı otomatik okur:
   ```bash
   cp config/application.properties.example config/application.properties
   ```
   Dosyadaki `CHANGE_ME` değerlerini kendi veritabanı parolan ve rastgele bir JWT anahtarınla değiştir (`openssl rand -base64 32`).
3. Uygulamayı başlat:
   ```bash
   ./mvnw spring-boot:run
   ```
4. Aç: http://localhost:8080/login.html — Swagger: http://localhost:8080/swagger-ui.html

İlk açılışta tablolar otomatik oluşur ve iki örnek saha (Saha 1, Saha 2) eklenir.

### Testler

```bash
./mvnw test
```

Servis testleri Mockito ile çalışır. Entegrasyon testleri için çalışan bir PostgreSQL gerekir.

## Ortam değişkenleri

Repodaki `application.properties` gizli bilgi içermez, değerleri ortamdan okur.

| Değişken | Açıklama | Varsayılan |
|---|---|---|
| `DB_URL` | JDBC bağlantı adresi | `jdbc:postgresql://localhost:5432/halisaha` |
| `DB_USERNAME` | Veritabanı kullanıcısı | `postgres` |
| `DB_PASSWORD` | Veritabanı parolası | zorunlu |
| `JWT_SECRET` | JWT imza anahtarı | zorunlu |
| `SHOW_SQL` | SQL loglama | `false` |
| `LOGIN_RATE_LIMIT_MAX_ATTEMPTS` | Pencere başına izin verilen başarısız giriş | `5` |
| `LOGIN_RATE_LIMIT_WINDOW_MINUTES` | Pencere süresi (dakika) | `15` |

## Docker ile çalıştırma

```bash
docker build -t halisaha-api .
docker run -p 8080:8080 \
  -e DB_URL=jdbc:postgresql://<host>:5432/<db> \
  -e DB_USERNAME=<kullanici> -e DB_PASSWORD=<parola> \
  -e JWT_SECRET=<anahtar> \
  halisaha-api
```

İmaj çok aşamalıdır (JDK ile derle, JRE ile çalıştır). Uygulama `PORT` değişkenini kullanır ve saat dilimi `Europe/Istanbul` olarak ayarlıdır, çünkü "geçmiş / gelecek" hesapları Türkiye saatine göre yapılır.

## Canlıya alma (Render + Neon)

1. Neon'da bir PostgreSQL veritabanı oluştur ve bağlantı bilgilerini al. Bağlantı adresine `?sslmode=require` ekle.
2. Render'da repoyu bağlayarak Docker tabanlı bir Web Service oluştur.
3. `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` ve `JWT_SECRET` değişkenlerini Render'ın Environment bölümüne ekle.
4. Her `git push` sonrası Render yeniden dağıtım yapar.

## Bilinen sınırlamalar

- Giriş denemesi sayacı bellekte tutulur. Uygulama yeniden başlayınca sıfırlanır ve birden fazla örnekle çalışırsa paylaşılmaz.
- Şema `ddl-auto=update` ile yönetilir. Üretimde Flyway gibi bir migrasyon aracı daha güvenlidir.
- Ücretsiz Render planında uygulama uyuyabilir.

## Geliştirici

Mustafa Kesim — [GitHub](https://github.com/MustafaKesim) · [LinkedIn](https://linkedin.com/in/mustafa-kesim-413ab029a)
