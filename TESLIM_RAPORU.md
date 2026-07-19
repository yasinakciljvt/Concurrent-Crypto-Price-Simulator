# Teslim Raporu — Eşzamanlı Kripto Fiyat Simülatörü

## 1. Grup Bilgileri

| Alan | Bilgi |
|---|---|
| Grup adı | Team Konkörınt |
| Grup üyeleri (5) | Ahmed Şamil Karadeniz, Yasin Akçil, Emirhan Kaya, Melis Kara, Ufuk Güneş |
| GitHub repo linki | https://github.com/yasinakciljvt/Concurrent-Crypto-Price-Simulator |
| Pull Request linkleri | https://github.com/yasinakciljvt/Concurrent-Crypto-Price-Simulator/pulls?q=is%3Apr |
| Conflict çözülen dosya | `SimulationEngine.java` — `runPass` süre ölçüm bloğu |
| Conflict çözüm commit / PR | `3d1f5c3` / PR #25 |
| Yapılan bonus | Java 21 Virtual Threads karşılaştırması (Bonus A) |

## 2. Kısa Açıklama

Sabit sayıda worker thread ile çok sayıda kripto fiyat güncelleme görevini işleyen bir Spring Boot
uygulaması. Aynı görev listesi hem güvensiz (korumasız) hem güvenli (`ReentrantLock` + `AtomicLong`)
sürümle işlenir; sonuçlar tek thread'de hesaplanan beklenen değerle karşılaştırılarak race
condition'ın etkisi ve güvenli çözümün doğruluğu invariant kontrolüyle kanıtlanır.

## 3. Çalıştırma (özet)

```bash
git clone https://github.com/yasinakciljvt/Concurrent-Crypto-Price-Simulator.git
cd Concurrent-Crypto-Price-Simulator
./mvnw spring-boot:run
# Swagger: http://localhost:8080/swagger-ui/index.html

# 1) Simülasyonu çalıştır (seed sabit -> tekrarlanabilir)
curl -X POST "http://localhost:8080/simulate?updates=10000&workers=4&seed=42"

# 2) Son simülasyonun istatistikleri
curl http://localhost:8080/stats

# 3) Son simülasyondaki güvenli coin durumu
curl http://localhost:8080/coins

# 4) Validation -> 400
curl -i -X POST "http://localhost:8080/simulate?updates=0&workers=4"
curl -i -X POST "http://localhost:8080/simulate?updates=-10&workers=4"
curl -i -X POST "http://localhost:8080/simulate?updates=10000&workers=100"
```

> Alternatif: Docker ile `docker build -t crypto-sim .` sonra `docker run --rm -p 8080:8080 crypto-sim`.

## 4. Tasarım Kararları (özet)

* **Task Pipeline & Görev Üretimi:** Görevler `TaskProducer` içinde seed'li `java.util.Random` ile **bir kez** ve **immutable** (`PriceUpdateTask` record + `List.copyOf`) üretilir; böylece güvenli ve güvensiz koşular birebir aynı iş yükünü kullanır ve aynı seed her makinede aynı listeyi verir.
* **Producer–Consumer & Kuyruk:** Görevler `executor.submit()` iç kuyruğuna bırakılmaz; sınırlı bir `LinkedBlockingQueue` (kapasite 10.000) üzerinden dağıtılır. Ayrı put/take kilitleri 1 producer + N consumer'da çekişmeyi azaltır; sınırlı kapasite backpressure sağlar. Tamamlanma **poison pill** ile bildirilir (worker sayısı kadar sentinel; her worker birini alıp temiz çıkar).
* **Worker Havuzu:** `WorkerEngine`, `Executors.newFixedThreadPool(workers, WorkerThreadFactory)` kullanır; her görev için yeni thread açılmaz. Thread'ler `worker-1` … `worker-N` olarak isimlendirilir.
* **Coin Kilidi & Sayaç:** Coin state'te 4 alan birlikte güncellendiği için `SafeCoinState` **coin başına `ReentrantLock`** kullanır; sayaç için tek alanlı `SafeCounter` (`AtomicLong`) yeterlidir. Güvensiz sürümler (`UnsafeCoinState`, `UnsafeCounter`) korumasızdır ve veri kaybını sergiler.
* **Tamamlanma & Graceful Shutdown:** `CountDownLatch(taskCount)` ile bitiş beklenir; havuz `shutdown()` + `awaitTermination()` ile kapatılır, timeout olursa `shutdownNow()` denenir (`try/finally` ile temizlik garanti).
* **Doğrulama:** `ExpectedResultCalculator` beklenen sonucu tek thread'de hesaplar, `InvariantChecker` güvenli sürümün bu hedefi tam karşıladığını (`safeInvariantPassed`) denetler.
* **Katman Ayrımı & 409:** Controller → Service → Engine. `SimulationService`, `AtomicBoolean.compareAndSet` + `finally` ile aynı anda tek simülasyonu garanti eder (ikinci istek 409).

## 5. Race Condition Kanıtı

```
Coin  | başlangıç | beklenen | güvenli    | güvensiz
BTC   | 60000     | 51502    | 51502  ✓   | 51990  ✗
ETH   | 3000      | 2978     | 2978   ✓   | 3131   ✗
SOL   | 150       | 85       | 85     ✓   | 66     ✗

safeInvariantPassed: true
```

## 6. Metrik Özeti

| Updates | Workers | Süre | Throughput | Invariant |
|---:|---:|---:|---:|---|
| 50.000 | 1 | 19 ms | 2.631.578 görev/sn | Başarılı |
| 50.000 | 2 | 30 ms | 1.666.666 görev/sn | Başarılı |
| 50.000 | 4 | 39 ms | 1.282.051 görev/sn | Başarılı |
| 50.000 | 8 | 41 ms | 1.219.512 görev/sn | Başarılı |

> Görevler çok kısa ve CPU-ağırlıklı olduğu için worker artışı hızlanma sağlamadı; kilit çekişmesi ve context-switch maliyeti paralellik kazancını dengeledi. (Ölçümler makineye göre değişir.)

## 7. Thread Dump Özeti

Simülasyon sırasında `WorkerThreadFactory` sayesinde `worker-1` … `worker-N` isimli sabit havuz thread'leri görülür (N = `workers` parametresi).
Boş kuyrukta worker'lar `LinkedBlockingQueue.take()` üzerinde WAITING (parking) durumundadır; görev işlerken RUNNABLE olurlar.
Deadlock gözlenmedi. Ayrıntı README'de.

## 8. Zorunlu Özellikler — Öz Değerlendirme

- [x] /simulate, /coins, /stats çalışıyor
- [x] Geçersiz parametre → HTTP 400, ikinci eşzamanlı istek → HTTP 409
- [x] Aynı görev listesi (immutable, tek üretim) safe ve unsafe'de kullanılıyor
- [x] BlockingQueue + sabit thread pool (her görev için yeni thread yok)
- [x] Güvensiz sürüm hatayı gösteriyor; güvenli sürüm invariant'ı sağlıyor
- [x] En az bir yerde ReentrantLock kullanıldı
- [x] Graceful shutdown; işlerin bitmesi bekleniyor
- [x] Seed ile tekrarlanabilir görev üretimi
- [x] throughput/süre + 1/2/4/8 worker tablosu
- [x] Thread dump alındı ve README'de yorumlandı
- [x] Swagger çalışıyor, adres README'de
- [x] Unit + en az 1 integration test (82 test)
- [x] En az 3 branch, 2 PR, 2 review, 1 çözülmüş conflict

## 9. Bireysel Katkı Tablosu

| Üye | Rol / Ne yaptı? | Branch | PR | Review |
|---|---|---|---|---|
| Ahmed Şamil Karadeniz | Simulation Core: `SimulationEngine` ana motor, `AtomicLong` destekli `SafeCounter` / `UnsafeCounter` yarış senaryoları, `ExpectedResultCalculator` ve `InvariantChecker` doğrulama testleri. | `feature/simulation-core` | #6 | #10, #11 |
| Emirhan Kaya | Worker Engine: `PriceWorker`, `WorkerThreadFactory` (`worker-N`), `WorkerEngine` (fixed pool, `CountDownLatch` completion, poison pill tüketimi, `shutdown`/`awaitTermination`), worker/shutdown/completion testleri; `SimulationEngine` entegrasyonu. | `feature/worker-engine` | #3, #8, #10, #12, #13 | #6 |
| Yasin Akçil | Task Pipeline: immutable `PriceUpdateTask` (record), seed'li deterministik `TaskProducer`, sınırlı `LinkedBlockingQueue` + poison pill (`TaskQueue`), producer thread'i (`TaskFeeder`), interrupt'ta güvenli pill gönderimi; `TaskProducerTest`, `TaskQueueTest`, `TaskFeederTest`; Bonus A (Virtual Threads karşılaştırması). | `feature/task-pipeline` | #7, #25 | #8, #16 |
| Melis Kara | API & Swagger: `SimulationController` (`/simulate`, `/coins`, `/stats`), DTO'lar, `GlobalExceptionHandler` (400/404/409/503), `@Min`/`@Max` validation, `AtomicBoolean` ile 409 kontrolü, Swagger; iş akışının controller'dan `SimulationEngine`'e taşınması. `SimulationControllerTest`, `SimulationControllerIntegrationTest`. | `feature/api` | #2, #11, #16, #18 | #4, #15 |
| Ufuk Güneş | Coin & State: `CoinState` tasarımı ve thread-safe implementasyonu, coin başına lock stratejisi, ilgili unit/integration testleri; Dockerize (`Dockerfile`, `.dockerignore`). | `feature/coin-state` | #20, #4, #29 | #7 |

## 10. Notlar (opsiyonel)

En çok zorlandığımız konu, ayrı branch'lerde paralel ilerlerken sonradan bu işleri birleştirip ana yapıda problemsiz çalıştırmaktı.

Projede esas tartışma, kimin neyi nasıl ve paralel olarak yapacağının baştan tam belirlenmemesinden kaynaklandı. Bunun haricinde önemli bir kararsızlık olmadı.

Gelecekte projeye eklenecek başlıca geliştirme, birden fazla farklı simülasyonu (farklı worker sayısı, seed veya coin sayısı) art arda ya da paralel koşturup performans-metrik ve invariant sonuçlarını topluca özetleyen bir kıyaslama raporu sistemi olurdu. Böylece konfigürasyonlar arasında otomatik ve detaylı karşılaştırma elde edilebilir.
