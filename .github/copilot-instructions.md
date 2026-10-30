# Copilot instructions for portfolio-valuation

This is a small Spring Boot 3.5 / Java 21 / Maven REST API that values investment
portfolios. It is used as a teaching repo, so favour clarity over cleverness.

## Project layout

- `controller/` – thin `@RestController` classes. No business logic here.
- `service/` – business logic (`PortfolioService`, `PricingService`, `FxService`).
- `repository/` – Spring Data JPA interfaces backed by in-memory H2.
- `model/` – JPA entities and Java `record` response types.
- Seed data lives in `src/main/resources/data.sql`.

## Conventions (must follow)

- **Money is always `BigDecimal`.** Never use `double` or `float` for amounts, prices,
  quantities or FX rates. Round with an explicit `RoundingMode` (`HALF_UP` to 2 dp for
  reported values). Never call `setScale(n)` without a rounding mode.
- **Constructor injection only.** No `@Autowired` on fields. One public constructor per bean.
- **Tests are required for every change.** New or changed behaviour needs a JUnit 5 test:
  - pure logic → plain unit test with Mockito (`@ExtendWith(MockitoExtension.class)`)
  - controller behaviour → `@WebMvcTest` slice with `@MockitoBean`
  - end-to-end → add a case to `PortfolioValuationApplicationTests` (`@SpringBootTest`)
  Use AssertJ (`assertThat`) and `isEqualByComparingTo` for `BigDecimal`.
- **Build must stay green.** `mvn -B verify` runs Spotless (google-java-format), the tests
  and a JaCoCo line-coverage gate of 85%. Run `mvn spotless:apply` before committing.
  Do not lower the coverage threshold or exclude classes from coverage to make the build pass.
- **Errors**: throw `NotFoundException` (404) for missing portfolios, prices or rates.
  Validation errors should produce 400 with a `ProblemDetail` body.
- **Currencies** are upper-case ISO 4217 codes. Supported: GBP, USD, EUR.
  Reject anything else with 400, not 500.
- **Keep the API consistent**: JSON over `/api/...`, plural resource names, `@PathVariable`
  for identity, `@RequestParam` for options (e.g. `?currency=EUR`).
- Do not add new dependencies without a comment in `pom.xml` explaining why.
- Do not change `Jenkinsfile`, `Dockerfile` or anything under `infra/` unless the issue asks.

## Working on an issue

1. Read the issue's acceptance criteria. Write the failing test first.
2. Make the smallest change in the right layer.
3. Run `mvn -B verify` and make sure it passes, including coverage.
4. In the PR description, list the files changed and how you tested it.
