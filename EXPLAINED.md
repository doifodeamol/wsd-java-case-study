# Feed Generator - explained like lemon squeezy
### (using the language of a cash equities trading desk)

## The story in one minute

A bank's website shows live prices of financial products. Prices arrive as a stream of lines, one per
**quote update**:

```
1352122280502,GB0005405286,101.23,1000,103.45,1000
timestamp     ISIN         bid   bidSize ask  askSize
```

In real life these come from an exchange or a market-data vendor. For development and testing we need
a **fake exchange** that produces such a stream, fast, on demand. That is what we build.

**Bid and ask in 20 seconds** (every trader knows this, juniors may not):

* **Bid** = the highest price a buyer is ready to pay. **Bid size** = how many shares they want.
* **Ask** = the lowest price a seller is ready to accept. **Ask size** = how many shares they offer.
* The gap between them (`ask − bid`) is the **spread**.

## Trading-desk picture

| On the desk                                                | In the code                            |
|------------------------------------------------------------|----------------------------------------|
| One quote update on the screen                             | one line of text                       |
| A market-data handler that produces one quote              | `CertificateUpdateTask` (a `Callable`) |
| The rules for a valid quote (price band, size limits)      | `CertificateUpdateFactory`             |
| The security identifier of a stock (e.g. HSBC)             | the ISIN → `IsinGenerator`             |
| The pool of N worker threads processing messages           | `ExecutorService` thread pool          |
| The component that hands out the work and collects results | `CertificateUpdateGenerator`           |
| The entry point / start-up command                         | `App.main`                             |

## The classes, one by one

### 1. `IsinGenerator` – the star of the case study
Every listed share has an **ISIN** (International Securities Identification Number): HSBC is
`GB0005405286`, Barclays `GB0031348658`, Apple `US0378331005`. It has 12 characters:
**2 letters (country) + 9 letters/digits + 1 check digit**.

**Why a check digit?** On a trading system a mistyped identifier is dangerous: you could send an order
for the wrong stock. The check digit is calculated from the other 11 characters, so a typo is caught
**before** the order goes anywhere. It is the same idea as the checksum on a card number.

Steps (this is the Luhn algorithm). Worked example, **HSBC `GB000540528` + check digit**:

1. Turn letters into numbers: `A=10, B=11 … G=16 … Z=35`. So `G=16`, `B=11`:
   `GB000540528` → `16 11 0 0 0 5 4 0 5 2 8`
2. Glue all digits together: `1611000540528` (13 digits)
3. Start from the **right**, **double every second digit** (the rightmost one included):

   ```
   digits:   1   6   1   1   0   0   0   5   4   0   5   2   8
   doubled:  2   6   2   1   0   0   0   5   8   0  10   2  16
   ```
4. Add up **every single digit** of the results (`10` counts as `1 + 0`, `16` as `1 + 6`):
   `2+6+2+1+0+0+0+5+8+0+1+0+2+1+6 = 34`
5. Check digit = how much is missing to the next multiple of 10 → `40 − 34 = 6` ✅
   so the full ISIN is `GB0005405286`.

**The fat-finger demo (do this live with your juniors):**

| Typed by the trader | What happened                    | Check digit says |
|---------------------|----------------------------------|------------------|
| `GB0005405286`      | correct                          | ✅ valid          |
| `GB0005405287`      | one digit typed wrong            | ❌ rejected       |
| `GB0005405826`      | two digits swapped (`28` → `82`) | ❌ rejected       |

**Trap for juniors:** if the sum is already a multiple of 10, such as `40`, the answer is `0`, not `10`.
The code does `(10 - sum % 10) % 10`. There is a test for it (SAP's ISIN `DE0007164600` ends in 0).

### 2. `CertificateUpdate` – one quote as an object
Holds the six fields. It is **immutable** (all fields `final`, no setters). Think of it like a message
on a Kafka topic: once published it is never edited, so any thread can read it with no locks.

**Prices are `BigDecimal`, not `double`.** In trading, `0.1 + 0.2` must be exactly `0.3`: wrong rounding
means wrong notional values and breaks at reconciliation. `double` also prints `100.0` instead of `100.00`.
We generate the price in whole **cents** (`10000…20000`) and let `BigDecimal.valueOf(cents, 2)` place the
decimal point. Always exactly 2 decimals, no rounding bugs.

### 3. `CertificateUpdateFactory` – the rules of a valid quote
Creates one random update inside the allowed ranges (price 100.00–200.00, bid size 1,000–5,000,
ask size 1,000–10,000). Two details worth teaching:

* `Random.nextInt(bound)` **excludes** the bound, but the spec ranges are **inclusive**.
  So we do `min + nextInt(max - min + 1)`. This is the classic *off-by-one at the price limit* bug.
* The random source and the clock are **passed in** (dependency injection). In production it is
  `ThreadLocalRandom` and the real time; in tests they are fixed values. It is the same reason trading
  systems can **replay** a recorded day of events and get the same result: no hidden randomness or
  hidden clock. That is why our tests can check the exact output.

### 4. `CertificateUpdateTask` – what the assignment asked for
`implements Callable<String>`; `call()` returns one line. That's the whole class. Small classes are easy to test.

Why `Callable` and not `Runnable`? `Callable` **returns a result** and may **throw** a checked exception.

### 5. `CertificateUpdateGenerator` – the work distributor
* Creates a pool with `Executors.newFixedThreadPool(threads)`.
* Hands over all tasks with `invokeAll` and waits for them.
* Reads every `Future` (`future.get()`) to collect the lines, **in order**.
* `finally { executor.shutdownNow(); }` – **always shut the pool down**, like a graceful service shutdown,
  otherwise the JVM may never exit.
* If the thread is interrupted, we do `Thread.currentThread().interrupt()` – we put the "please stop"
  flag back instead of swallowing it.
* If a task fails, the real error is passed on as the `cause`.

### 6. `App` – the entry point
Reads the 2 arguments, runs the generator, prints the lines. The printing logic is in `run(args, out)`
so a test can capture the output instead of reading the console.

## Why is this problem easy to run in parallel?
Each quote is **independent**: quote 7 does not care about quote 6. So the threads never need to
coordinate, and the tasks never share changeable data.

Compare with an **order**: it moves through states (new → validated → routed → filled → settled) and
every event must be applied **in the right order** for that order. That needs a state machine and
ordered queues per order. Same threads, much harder problem. Good question to ask your juniors:
*"What would change if each line depended on the previous one?"*

## Thread-safety: the one thing to remember
`ThreadLocalRandom.current()` must be called **inside the thread that uses it**. Never store it in a field
and share it. That's why we pass a *supplier* (`ThreadLocalRandom::current`) and call it at the moment
we need a number. Then every worker thread automatically gets its own random generator, with no locking.

All our objects are either **immutable** or **stateless**, so there is nothing to lock.

## A good "senior" observation about the data
In the specification, bid and ask are **independent random numbers** in the same range. So about half the
time the bid is **higher** than the ask. On a real market that is called a *crossed book* and it should
not happen: bid ≤ ask. We follow the specification (and a test would not expect otherwise), but it is
worth saying out loud in a review: *"fine for a simulator, I would not accept this in a real feed."*

## Bugs we found in the starting code

| Where | Problem | Fix |
|-------|---------|-----|
| `App.main` | After a successful run it **always** threw `RuntimeException` (the `throw` was outside the `if`). It also printed the array object (`String[]@1a2b`), not its content. | Validate first, then run. |
| `CertificateUpdateGenerator` | `generateQuotes()` produced empty placeholder objects (`CertificateUpdate` had no fields) and `limit(quotes)` ignored `threads`, so it gave 100 items where the existing test expects 1000. | Real implementation using a thread pool. |
| `App` | An empty constructor `App(String, String)` that did nothing. | Removed. |
| `pom.xml` | `source/target 11` can silently use newer JDK APIs. | `release 11` makes the compiler enforce the Java 11 API. |

## One decision to be aware of (and easy to change)
The README says "number of certificate updates", the provided test expects **threads × quotes**
(10 threads, 100 → 1000 lines). We followed the provided test: *each thread produces `quotes` updates*.
If your reviewers mean "total lines", it is a one-line change in `CertificateUpdateGenerator.runAll`
(replace `Math.multiplyExact(threads, quotes)` by `quotes`) plus the existing test.

## How to run

```bash
mvn test                                   # unit tests
mvn -q compile
java -cp target/classes com.solvians.showcase.App 4 5   # 4 threads x 5 updates = 20 lines
```

## Cheat-sheet to say out loud to juniors
1. **Small classes, one job each.** Easy to test, easy to explain.
2. **Inject what's random or time-based.** Then tests are exact and replayable, not "probably fine".
3. **Money is never `double`.** Use `BigDecimal` (or integer cents/ticks).
4. **Immutable objects need no locks.** Think "a published message is never edited".
5. **Always shut down your executor** (in `finally`).
6. **Never swallow `InterruptedException`.**
7. **Test the edges**: min, max, zero, and "sum is a multiple of 10".
8. **Validate identifiers at the door.** A check digit catches the fat-finger before it costs money.
9. **Don't trust starter code**: read it. Ours threw an exception even on success.
