# Eşzamanlı Kripto Fiyat Simülatörü

## Proje Hakkında

Bu proje, bellek içinde (in-memory) çalışan eşzamanlı bir kripto fiyat simülasyon uygulamasıdır. Üretilen N adet fiyat güncelleme görevi bir kuyruğa iletilir ve sabit sayıda iş parçacığından (worker) oluşan bir havuz tarafından eşzamanlı olarak tüketilir. Paylaşılan ortak sayaçlar ve coin durumları (coin state) üzerinde eşzamanlı erişimdeki yarış durumları (race condition) gösterilmiş; aynı iş yükü hem güvensiz hem güvenli çalıştırılıp karşılaştırılarak thread-safety güvenle sağlanmıştır.

## Kullanılan Teknolojiler

Java 21, Spring Boot 4.1.0, Maven, Git/GitHub, Swagger/OpenAPI (springdoc-openapi-ui 2.x), JUnit 5.

## Uygulamayı Çalıştırma

### Docker ile Çalıştırma (Dockerize Metodu)

1. Projeyi indirin:
   ```bash
   git clone https://github.com/yasinakciljvt/Concurrent-Crypto-Price-Simulator.git
   ```
2. Proje klasörüne girin:
   ```bash
   cd Concurrent-Crypto-Price-Simulator
   ```
3. Docker image'ını oluşturun:
   ```bash
   docker build -t concurrent-crypto-simulator .
   ```
4. Uygulamayı Docker container'ı içinde başlatın:
   ```bash
   docker run --rm -p 8080:8080 concurrent-crypto-simulator
   ```
5. Uygulama başladıktan sonra Swagger arayüzüne erişmek için tarayıcınızda `http://localhost:8080/swagger-ui/index.html` adresini açabilirsiniz.

> Gerekli: [Docker](https://www.docker.com/products/docker-desktop/) sisteminizde kurulu olmalıdır.

## Swagger Adresi

- http://localhost:8080/swagger-ui/index.html
- OpenAPI: http://localhost:8080/api-docs
- Endpoint'ler Swagger üzerinden test edilebilir.

## Endpoint'ler

| Endpoint | Ne yapar? |
|---|---|
| `POST /simulate?updates=10000&workers=4&seed=42` | Coin durumlarını sıfırlar, seed ile immutable görev listesini üretir, beklenen sonucu tek thread'de hesaplar, ardından **aynı listeyle** önce güvensiz sonra güvenli simülasyonu çalıştırır. Görevler `BlockingQueue` üzerinden sabit worker havuzuna dağıtılır; bütün görevler bitmeden cevap dönmez, havuz graceful şekilde kapatılır. `updates` 1–100.000, `workers` 1–16, `seed` opsiyoneldir (verilmezse üretilir ve cevapta döner). |geçersiz parametre · 409 zaten çalışan simülasyon · 503 zaman aşımı |
| `GET /coins` | Son **tamamlanan** simülasyondaki güvenli coin durumları: `initialPrice`, `currentPrice`, `updateCount`, `lastDelta`, `lastUpdatedBy`. Henüz simülasyon çalışmadıysa başlangıç değerleri döner. | 
| `GET /stats` | Son tamamlanan simülasyonun sonucu: kullanılan seed, gönderilen görev sayısı, worker sayısı, güvenli/güvensiz işlenen görev sayıları, süreler, throughput değerleri, invariant sonucu ve her coin için başlangıç/beklenen/güvensiz/güvenli fiyat ile güncelleme sayıları. | 


## Mimari Akış


HTTP → SimulationController → SimulationService → SimulationEngine ↓
              
TaskProducer ──► TaskQueue ──► Worker Pool (N) ──► Coin State + Sayaçlar
                (BlockingQueue)   worker-1..N

`SimulationController` yalnızca HTTP sözleşmesinden sorumludur: routing, `@Min`/`@Max` ile parametre
doğrulama ve Swagger dokümantasyonu; iş mantığı içermez. `SimulationService` uygulama durumunu yönetir;
`AtomicBoolean` ile aynı anda tek simülasyon çalışmasını garanti eder, tamamlanan koşunun sonucunu
`volatile` alanlarda saklayıp `/coins` ve `/stats` isteklerine sunar, DTO dönüşümünü ve özet loglamayı
yapar. Böylece eşzamanlılık mantığı web katmanından tamamen yalıtılmıştır.

`TaskProducer` belirtilen seed ile immutable fiyat değişim görevlerini bir kez üretir; `TaskFeeder` bunları sınırlı bir `LinkedBlockingQueue`'ya akıtır. `WorkerEngine` havuzundaki worker'lar (`PriceWorker`) görevleri kuyruktan çekip coin fiyatlarını günceller. `SimulationEngine` tüm akışı koordine eder; `SafeCounter`/`UnsafeCounter` ile yarış durumunu ölçer, `InvariantChecker` ile simülasyon sonu tutarlılığı doğrular.


`WorkerThreadFactory` thread'leri `worker-N` diye adlandırır; `PriceWorker` kuyruktan görev alıp işler, poison pill görünce çıkar. `WorkerEngine` sabit havuzu, `CountDownLatch` ile tamamlanmayı ve `shutdown`/`awaitTermination` ile graceful kapanışı yönetir.

## ⭐ Tasarım Kararları


> Yönergedeki 9 karar noktasının her biri için aracınızı ve **kısa "neden"ini** yazın.


`TaskProducer` belirtilen seed ile immutable fiyat değişim görevlerini bir kez üretir; `TaskFeeder` bunları sınırlı bir `LinkedBlockingQueue`'ya akıtır. `WorkerEngine` havuzundaki worker'lar (`PriceWorker`) görevleri kuyruktan çekip coin fiyatlarını günceller. `SimulationEngine` tüm akışı koordine eder; `SafeCounter`/`UnsafeCounter` ile yarış durumunu ölçer, `InvariantChecker` ile simülasyon sonu tutarlılığı doğrular.
`SimulationController` yalnızca HTTP sözleşmesini taşır: routing, `@Min`/`@Max` ile parametre doğrulama ve Swagger dokümantasyonu; iş mantığı içermez. `SimulationService` uygulama durumunu yönetir; `AtomicBoolean.compareAndSet` ile aynı anda tek simülasyon çalışmasını garanti eder — kontrol ve yazma tek atomik adımda olduğu için iki isteğin arasına girip ikisinin de geçmesi mümkün değildir — ve bayrağı `finally` bloğunda serbest bırakarak hata durumunda sistemin kilitli kalmasını önler. Tamamlanan koşunun sonucu `volatile` alanlarda (`lastStats`, `lastCoins`) immutable snapshot olarak saklanır; okuyucular hiç kilitlenmeden ya önceki tamamlanmış sonucu ya da yeni sonucu görür, yarım state göremez. `GlobalExceptionHandler` bütün hataları tek tip `ErrorResponseDto` gövdesine çevirir: geçersiz parametre 400, çalışan simülasyon 409, henüz sonuç yokken `/stats` 404, zaman aşımı 503. Bu ayrım sayesinde thread pool ve kuyruk mantığı web katmanından tamamen yalıtılır; `SimulationEngine` Spring'e ve HTTP'ye bağımlı olmadan test edilebilir.

## ⭐ Tasarım Kararları
| Karar noktası | Kararımız | Neden? (+alternatif karşılaştırması) |
|---|---|---|
| Görev kuyruğu | Sınırlı `LinkedBlockingQueue` (kapasite 10.000) | Ayrı put/take kilitleri → 1 producer + N consumer'da çekişme az (`ArrayBlockingQueue` tek kilit kullanır). Sınırlı kapasite backpressure sağlar, 100.000 görevde bellek patlamaz. |
| Worker havuzu | `newFixedThreadPool(workers)` + özel `WorkerThreadFactory` | Sabit havuz thread'leri yeniden kullanır; her görev için `new Thread()` açmak thread oluşturma maliyeti, bellek ve context-switch yükü getirirdi. Worker sayısı endpoint'ten kontrol edilir. |
| Güvenli sayaç | `AtomicLong` (`SafeCounter`) | Tek alanlı sayaçta CAS tabanlı `incrementAndGet()` kilitsiz ve yeterli; `synchronized`'a gerek yok. |
| Coin kilidi | `ReentrantLock` (`SafeCoinState`) | Coin'de fiyat + updateCount + lastDelta + lastUpdatedBy birlikte (compound action) güncelleniyor; tek `AtomicLong` yetmez, blok seviyesinde kilit şart. |
| Lock kapsamı | Coin başına ayrı lock | Her `CoinState` kendi lock'unu tutar; BTC güncellenirken ETH/SOL beklemez. Tek global lock doğru ama gereksiz çekişme yaratırdı. |
| İşlerin tamamlanması | Poison pill + `CountDownLatch` | Producer sonuna worker sayısı kadar sentinel koyar, her worker birini alıp temiz çıkar; latch tüm görevlerin işlendiğini bekler. Interrupt gerekmez. |
| Graceful shutdown | `shutdown()` + `awaitTermination()`, timeout'ta `shutdownNow()` | Yeni iş kabulü durur, çalışanlar beklenir; süre aşılırsa `shutdownNow()` ile kesilir. |
| Sonucun paylaşılması | `volatile` immutable snapshot (`SimulationReport` → DTO, `lastStats`/`lastCoins`) | Controller son sonucu değişmez bir snapshot olarak okur; canlı repo okunmadığı için yarı-güncel durum görünmez. |
| İkinci simülasyon isteği | `AtomicBoolean.compareAndSet` + `finally` | Aynı anda tek simülasyon; hata olsa bile `finally`'de serbest bırakılır, ikinci istek 409 alır. |
| Katman ayrımı | Controller → Service → Engine | Controller yalnızca HTTP sözleşmesini taşır (routing, validation, Swagger); durum yönetimi `SimulationService`'te, eşzamanlılık akışı `SimulationEngine`'de. İş akışı controller'da kalsaydı thread pool mantığı web katmanına sızar, engine HTTP'den bağımsız test edilemezdi. |
| Parametre doğrulama | `@Validated` + `@Min`/`@Max` (`updates` 1–100.000, `workers` 1–16) | Sınırlar deklaratif olarak endpoint imzasında durur, Swagger'a da otomatik yansır. Elle `if` kontrolü yazmak aynı işi yapardı ama dokümantasyona yansımaz ve her endpoint'te tekrarlanırdı. |
| Hata cevapları | `GlobalExceptionHandler` + tek tip `ErrorResponseDto` | Tüm hatalar aynı gövde şeklinde döner: 400 geçersiz parametre, 409 çalışan simülasyon, 404 henüz sonuç yok, 503 zaman aşımı. Her endpoint'te `try/catch` yazmak yerine tek noktada toplanır. |
| Simülasyon öncesi cevaplar | `/coins` 200 (başlangıç değerleri), `/stats` 404 | Coin'ler uygulama açılışında tanımlıdır, başlangıç fiyatları anlamlı bir durumdur; istatistik ise ancak koşu sonucunda üretilir. `/stats` için 200 + boş gövde istemciyi her cevapta "dolu mu boş mu" kontrolüne zorlardı. |



## Race Condition Gözlemi
İki race noktası gösterildi: (1) işlenen görev sayacı `count++` (oku-artır-yaz üç adımdır, iki thread aynı değeri okursa bir artırım kaybolur); (2) coin state'te birden fazla alanın tutarsız güncellenmesi.

Gerçek çıktı (50.000 updates, 8 worker):
Sayaç beklenen: 50.000 | güvenli: 50.000 ✓ | güvensiz: 49.980 ✗
BTC beklenen: 54.885 | güvenli: 54.885 ✓ | güvensiz: 54.833 ✗
ETH beklenen: 638 | güvenli: 638 ✓ | güvensiz: 617 ✗
SOL beklenen: -139 | güvenli: -139 ✓ | güvensiz: -137 ✗

> **Gözlem:** 50.000 updates + 8 worker ile yapılan **20 çalıştırmanın 20'sinde** güvensiz sonuç bozuldu (sayaç veya en az bir coin beklenenden saptı). Worker ve görev sayısı arttıkça lost-update olasılığı arttı; düşük worker sayısında (1 worker) çekişme olmadığı için güvensiz sonuç zaman zaman doğru da çıkabiliyor. Güvenli sürüm her çalıştırmada invariant'ı sağladı.

## Güvenli Çözüm

Coin durumunu korumak için `ReentrantLock` kullanıldı. Tek başına `AtomicLong` yalnızca tek bir değişkeni (örn. sadece fiyatı) korur; ancak coin durumunda fiyat, güncelleme sayısı, son delta ve son güncelleyen thread aynı anda ve atomik bir bütün (compound action) olarak güncellenmelidir. Bu yüzden dört alan tek bir kritik bölümde kilitlenir.
```java
lock.lock();
try {
    currentPrice += delta;
    updateCount++;
    lastDelta = delta;
    lastUpdatedBy = Thread.currentThread().getName();
} finally {
    lock.unlock();
}

## Invariant ve Doğruluk Kanıtı

'''
safePrice       == initialPrice + sum(all deltas)    ->  BAŞARILI (Geçti)
safeUpdateCount == o coin için üretilen görev sayısı ->  BAŞARILI (Geçti)
```
SimulationEngine, güvenli koşu bittiğinde her coin için bu iki invariant'ı ve safeCounter == submittedUpdates eşitliğini InvariantChecker ile denetler; safeInvariantPassed alanı sonuçta döner.

## Performans Sonuçları

| Updates | Workers | Süre | Throughput | Invariant |
|---:|---:|---:|---:|---|
| 50.000 | 1 | 11 ms | 4.545.454 görev/sn | Başarılı |
| 50.000 | 2 | 14 ms | 3.571.428 görev/sn | Başarılı |
| 50.000 | 4 | 14 ms | 3.571.428 görev/sn | Başarılı |
| 50.000 | 8 | 14 ms | 3.571.428 görev/sn | Başarılı |

<Yorum: Worker sayısı arttıkça süre kısalmış ve throughput (saniyede işlenen görev sayısı) artmıştır. Ancak worker sayısı işlemci çekirdek sınırına ulaştığında kilit çekişmesi (lock contention) ve thread'ler arası geçiş maliyeti (context switch) arttığı için performans kazancı bir noktadan sonra durmuş, hatta süre hafifçe uzamıştır.>

## ReentrantLock ve synchronized Karşılaştırması

<Nerede hangisini kullandınız? ReentrantLock'un sağladığı ekstralar (tryLock, adalet, kesintiye
uğrayabilir kilitleme) sizin için gerekli miydi? Global lock vs coin başına lock farkı.>

## Thread Dump İncelemesi

Simülasyon sırasında IntelliJ "Capture Thread Dump" / `jstack` / `jcmd` ile alınır.
`WorkerThreadFactory` sayesinde havuz thread'leri `worker-1` … `worker-N` olarak görünür.

```
"worker-1" ... RUNNABLE ...
"worker-2" ... WAITING (parking) ... at java.util.concurrent.LinkedBlockingQueue.take(...)
"worker-3" ... WAITING (parking) ... at java.util.concurrent.LinkedBlockingQueue.take(...)
"worker-4" ... RUNNABLE ...
```

> Not: Yukarıdaki kesit tipik gözlemdir; gerçek dump çıktınızı buraya yapıştırabilirsiniz.

- **Kaç worker var?** Havuz boyutu workers parametresiyle (örneğin 4) tam uyumludur. Sistemde kontrolsüz thread oluşturulup bellek tüketilmemiş, sabit havuz korunmuştur.
- **Hangi state'teler?** Kuyrukta görev kalmadığında worker thread'leri `BlockingQueue.take()` metodunda `WAITING (parking)` durumuna geçerek CPU tüketmeden yeni görevin gelmesini beklemektedir.
- **Lock çekişmesi/deadlock var mı?** Coin'ler bağımsız kilitlendiği ve döngüsel bir kilit sırası (circular wait) oluşmadığı için herhangi bir deadlock (`BLOCKED`) durumu tespit edilmemiştir.

## Merge Conflict Deneyimi

- **Branch isimleri:** `perf/high-precision-metrics` (#24) ve `feature/timing-scope-yasin` (#25)
- **Conflict çıkan dosya / bölüm:** `SimulationEngine.java` — `runPass` metodundaki süre ölçüm bloğu
- **İki branch'in farklı değişikliği:** #24 ölçümü `System.currentTimeMillis()`'ten `System.nanoTime()`'a çevirdi (hassasiyet artışı); #25 ise ölçümü feeder başlatıldıktan sonraya taşıdı (kapsam — kuyruk ve thread kurulumu ölçüm dışında bırakıldı). İkisi de aynı `start` satırına dokundu: biri satırı değiştirdi, diğeri sildi/taşıdı. Git otomatik birleştiremedi.
- **Hangi içerik korundu:** İkisi de birleştirildi — `nanoTime()` hassasiyeti, feeder'dan sonraki işleme penceresine uygulandı (`startNanos` artık `startWorkers` ve `TaskFeeder.start()` çağrılarından sonra alınıyor). Tek bir tarafı seçmek iki durumda da yanlıştı: #25'i seçmek `startNanos` referansını tanımsız bırakıp derlemeyi kırıyordu, #24'ü seçmek ise ölçüm kapsamı iyileştirmesini kaybettiriyordu.
- **IntelliJ mi terminal mi:** Terminal (`git merge origin/develop`) ile conflict çıkarıldı, çözüm dosya üzerinde elle yapıldı; sonuç `mvn test` ile doğrulandı.
- **Çözüm commit / PR linki:** `3d1f5c3` — "Resolve timing conflict: nano precision on processing window"; [PR #25](https://github.com/yasinakciljvt/Concurrent-Crypto-Price-Simulator/pull/25)
- **Ne öğrendik:** Aynı koda iki farklı iyileştirme geldiğinde Git otomatik seçim yapamıyor ve naif "birini seç" çözümü kodu bozabiliyor (biri derleme hatası, diğeri sessiz mantık hatası). Doğru çözüm iki niyeti anlayıp birleştirmekti. Ayrıca conflict çözümünden sonra `mvn test` çalıştırmanın kritik olduğunu gördük — çünkü `startNanos`/`elapsedNanos` tutarsızlığı ancak derleme/test aşamasında yakalanıyordu.


## Testler

`./mvnw test` ile çalıştırılır.

- `WorkerThreadFactoryTest` — thread adları `worker-N`
- `PriceWorkerTest` — kuyruk tüketimi + poison pill ile durma
- `WorkerEngineTest` — sabit havuz ile görev işleme
- `WorkerCompletionTest` — `CountDownLatch` ile tamamlanma
- `WorkerShutdownTest` — graceful shutdown / `awaitTermination`
- `WorkerThreadFactoryTest` — thread adları `worker-N`
- `PriceWorkerTest` — kuyruk tüketimi + poison pill ile durma
- `WorkerEngineTest` — sabit havuz ile görev işleme
- `TaskProducerTest` — seed tekrarlanabilirliği, immutability, parametre sınırı
- `TaskQueueTest`, `TaskFeederTest` — kuyruk, backpressure, poison pill, interrupt
- `ExpectedResultCalculatorTest` — beklenen fiyat hesabı
- `CounterTest` — güvenli/güvensiz sayaç
- `CoinStateTest`, `SafeCoinStateTest`, `CoinInvariantTest` — coin state + invariant
- `InvariantCheckerTest`, `StatsTest`, `SimulationEngineTest` — motor ve doğrulama
- `InMemoryCoinRepositoryTest` — coin deposu
- `SimulationControllerTest` — endpoint sözleşmesi: 404/400 senaryoları, simülasyon sonrası `/stats` ve `/coins`, seed tekrarlanabilirliği
- `SimulationControllerIntegrationTest` — uçtan uca akış: simülasyon çalıştırma ve üç endpoint'in tutarlılığı
- Coin invariant testi
- `CoinStateTest` ve `SafeCoinStateTest` — coin durumlarının (güvenli/güvensiz) güncellenmesi, doğru lock ile race condition'ın önlenmesi test edilir.
- `CoinInvariantTest` — her coin için tutarlılık (invariant) koşullarının simülasyon sonunda geçip geçmediği kontrol edilir.

## Grup Üyeleri ve Katkıları

| Üye | Sorumluluk | Branch | Pull Request | Review |
|---|---|---|---|---|
| Ahmed Şamil Karadeniz | Simulation core modülü, sayaçlar ve invariant kontrolü: race condition analizi, güvenli/güvensiz sayaç testi, invariant doğrulama birimleri | `feature/simulation-core` | PR #6 | PR #10, #11 |
| Emirhan Kaya | Worker Engine: `PriceWorker`, `WorkerThreadFactory` (`worker-N`), `WorkerEngine` (fixed pool, completion/shutdown), ilgili unit testler | `feature/worker-engine` | PR #3, #8, #10, #12, #13 | PR #6 |
| Yasin Akçil | Task Pipeline: `PriceUpdateTask` (immutable record), `TaskProducer` (seed'li deterministik üretim), `TaskQueue` (sınırlı `LinkedBlockingQueue` + poison pill), `TaskFeeder` (producer thread'i), interrupt'ta güvenli pill gönderimi (`offerPoisonPills`), ilgili unit ve pipeline integration testleri | `feature/task-pipeline` | PR #7 | PR #8, #16 |
| Melis Kara | API Modülü: `SimulationController` (`/simulate`, `/coins`, `/stats`), DTO'lar ve `from()` eşlemeleri, `GlobalExceptionHandler` (400/404/409), `@Min`/`@Max` validation, `AtomicBoolean` ile 409 kontrolü, Swagger dokümantasyonu; simülasyon iş akışının `SimulationEngine`'e taşınması ve durum yönetiminin `SimulationService`'e ayrıştırılması, controller ve integration testleri | `feature/api` | PR #2, #11, #16, #18 | PR #4, #15 |
| Ufuk Güneş | Coin & State Uygulaması, `CoinState` sınıfının tasarımı ve thread-safe implementasyonu, coin başına lock stratejisi, ilgili unit/integration testleri, feature branch süreci, review katkıları | `feature/coin-state` | PR #20, #4 | PR #7 |

## Bonus Çalışmalar

`VirtualThreadComparisonTest` ile aynı 50.000 görev, 8 thread'li sabit havuzda ve Java 21 virtual-thread-per-task executor ile çalıştırılarak süreleri karşılaştırıldı ve iki yöntemin de bütün görevleri işlediği doğrulandı. Proje ayrıca multi-stage `Dockerfile` ve `.dockerignore` kullanılarak Dockerize edildi.
