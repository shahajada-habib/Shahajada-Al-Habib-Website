# Thymeleaf, explained for someone who's never used it

You know HTML, CSS, and vanilla JS well (that's what the other boilerplates use). Thymeleaf is different in one specific way: **the HTML is filled in on the server, before it ever reaches the browser.** There's no `fetch()` call that loads data after the page appears — by the time your browser sees the page, the real title, the real article text, the real image are already sitting in the HTML. That's the whole reason this boilerplate uses it: Google and Facebook's link-preview bots don't run JavaScript, so they need to see real content immediately, not an empty shell that fills in later.

Everything below assumes you're looking at files under `src/main/resources/templates/`.

## The five things you'll actually use

**1. `th:text` — put a value inside a tag**
```html
<h1 th:text="${article.title}">Placeholder</h1>
```
Renders as `<h1>Whatever the real title is</h1>`. The word "Placeholder" is only there so the file still looks right if you double-click it and open it directly in a browser (Thymeleaf files are valid HTML on their own).

**2. `th:utext` — same thing, but don't escape HTML**
Used exactly once in this project: `article.html`'s `th:utext="${article.content}"`, because article content comes from the rich-text editor and already contains real `<p>`, `<strong>`, `<img>` tags that need to render as HTML, not as literal text.

**3. `th:if` / `th:unless` — show something conditionally**
```html
<span th:if="${article.subtitle != null}" th:text="${article.subtitle}"></span>
```
If the condition is false, the whole tag is removed from the output — not just hidden with CSS.

**4. `th:each` — loop over a list**
```html
<a th:each="item : ${latest}" th:href="@{'/article/' + ${item.slug}}" th:text="${item.title}"></a>
```
This one `<a>` tag in the source file becomes N `<a>` tags in the output, one per item in `latest`.

**5. `@{...}` — build a URL**
```html
<a th:href="@{'/category/' + ${cat.slug}}">
```
Just string concatenation for links. You'll see this everywhere instead of plain `href="..."`.

That's genuinely most of what you need to read and edit these templates. Everything else in the project is small variations on these five patterns.

## Where a page's content actually comes from

Every page has two halves:

1. **The `.html` template** (`src/main/resources/templates/*.html`) — the layout/markup.
2. **`PageController.java`** (`src/main/java/com/blogcms/web/PageController.java`) — decides *which* template to render and *what data* to hand it.

Example: visiting `/article/some-slug` runs `PageController.article()`, which fetches the article, its comments, and related posts, puts them in the `Model`, and returns the string `"article"` — Thymeleaf then finds `templates/article.html` and fills it in using that data.

**If you want to add a new field to a page** (say, an estimated reading time): compute it in `PageController.java`, add it to the model (`model.addAttribute("readingTime", ...)`), then reference `${readingTime}` in the template. The data always has to be added on the Java side first — templates can't reach into the database themselves.

## The shared header/footer (fragments)

`templates/fragments/layout.html` holds three reusable pieces, each marked with `th:fragment="name"`:
- `head(title, description, image, url)` — the `<head>` tag, parameterized so every page can set its own title/meta tags
- `header` — logo, search box, category nav, breaking news ticker
- `footer`

Every page includes them like this:
```html
<head th:replace="~{fragments/layout :: head(${pageTitle}, ${pageDescription}, ${pageImage}, ${pageUrl})}"></head>
...
<div th:replace="~{fragments/layout :: header}"></div>
```
Edit the nav links, logo, or footer copy **once** in `fragments/layout.html` and it changes on every page.

## Common edits, step by step

**Change the site name** ("Aurora Herald" → your client's name): it appears in `fragments/layout.html` (logo + title suffix), `SecurityConfig` isn't involved, and in `RssFeedController.java` / a couple of `<title>` defaults. Simplest approach: search the whole `backend/` folder for "Aurora Herald" and replace every match.

**Change colors/fonts**: `src/main/resources/static/css/style.css`, the `:root { --primary: ...; }` block at the top. Nothing else needs to change — every page pulls from these variables.

**Add a brand-new page** (e.g. `/about`):
1. Add a method to `PageController.java`: `@GetMapping("/about") public String about(Model model) { ...; return "about"; }`
2. Create `templates/about.html` (copy `search.html` as a starting skeleton — it's the simplest one)
3. Add `/about` to the `permitAll()` list in `SecurityConfig.java` (look for the line listing `"/", "/category/**", "/article/**"...`) — otherwise Spring Security will demand a login for it.

**Add a field to the article page**: it's almost certainly already on `NewsResponseDto` (check `news/NewsResponseDto.java` — subtitle, SEO fields, image metadata, tags are all there). If it's already on the DTO, just reference it in `article.html` with `th:text` or `th:if`. If it's genuinely new, it needs to be added to the `News` entity, a Flyway migration (`src/main/resources/db/migration/`), `NewsRequestDto`/`NewsResponseDto`, and `NewsService`'s mapping methods — that's a real backend change, not just a template edit.

## Running it locally

```bash
cd backend
./mvnw spring-boot:run
```
Uses the `dev` profile by default: in-memory H2 database, reset on every restart, seeded with 5 categories and three logins (`admin` / `editor` / `reporter`, all password `1234`). Visit `http://localhost:8081` for the public site, `http://localhost:8081/admin` for the CMS.

For a persistent local database, run with `mysql-dev` profile instead (`SPRING_PROFILES_ACTIVE=mysql-dev`) — see `application-mysql-dev.properties`.

## Before deploying for a real client

Set the `SITE_URL` environment variable to the real domain — `sitemap.xml`, `rss.xml`, and Open Graph tags all fall back to `https://your-domain.example.com` otherwise, which is obviously wrong in production.
