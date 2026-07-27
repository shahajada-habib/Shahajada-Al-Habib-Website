// ---- API helper (same-origin, no separate API_BASE needed) ----
async function apiRequest(path, { method = "GET", body, isForm = false } = {}) {
  const headers = {};
  const token = localStorage.getItem("adminToken");
  if (token) headers["Authorization"] = "Bearer " + token;
  if (!isForm) headers["Content-Type"] = "application/json";

  const res = await fetch(path, {
    method,
    headers,
    body: isForm ? body : body ? JSON.stringify(body) : undefined
  });

  let data = null;
  try { data = await res.json(); } catch (e) { /* no body */ }

  if (!res.ok) {
    throw new Error((data && (data.message || data.error)) || "Request failed (" + res.status + ")");
  }
  return data;
}

function isLoggedIn() {
  return !!localStorage.getItem("adminToken");
}

// ---- login/logout ----
document.getElementById("login-form").addEventListener("submit", async (e) => {
  e.preventDefault();
  const errorEl = document.getElementById("login-error");
  errorEl.textContent = "";
  const formData = new FormData(e.target);
  try {
    const res = await apiRequest("/api/auth/login", {
      method: "POST",
      body: { username: formData.get("username"), password: formData.get("password") }
    });
    localStorage.setItem("adminToken", res.token);
    localStorage.setItem("adminUsername", res.username);
    showDashboard();
  } catch (err) {
    errorEl.textContent = err.message || "Login failed.";
  }
});

document.getElementById("logout-btn").addEventListener("click", () => {
  localStorage.removeItem("adminToken");
  window.location.reload();
});

function showDashboard() {
  document.getElementById("login-view").style.display = "none";
  document.getElementById("dashboard-view").style.display = "block";
  initQuill();
  loadDashboard();
  loadCategoriesForSelect();
  loadArticles();
  loadComments();
  loadCategories();
  loadMedia();
}

// ---- tabs ----
document.querySelectorAll("[data-tab]").forEach((btn) => {
  btn.addEventListener("click", () => {
    document.querySelectorAll("[data-tab]").forEach((b) => b.classList.remove("active"));
    document.querySelectorAll(".tab-panel").forEach((p) => p.classList.remove("active"));
    btn.classList.add("active");
    document.getElementById(btn.dataset.tab + "-panel").classList.add("active");
  });
});

function statusBadge(status) {
  return `<span class="badge ${status}">${status}</span>`;
}

// ---- Dashboard ----
async function loadDashboard() {
  try {
    const stats = await apiRequest("/api/admin/dashboard/stats");
    document.getElementById("stat-grid").innerHTML = `
      <div class="stat-card"><div class="value">${stats.totalNews}</div><div class="label">Total articles</div></div>
      <div class="stat-card"><div class="value">${stats.published}</div><div class="label">Published</div></div>
      <div class="stat-card"><div class="value">${stats.draft}</div><div class="label">Drafts</div></div>
      <div class="stat-card"><div class="value">${stats.pendingComments}</div><div class="label">Pending comments</div></div>
      <div class="stat-card"><div class="value">${stats.todayPublished}</div><div class="label">Published today</div></div>
    `;
    document.getElementById("dashboard-lists").innerHTML = `
      <h3 style="margin-bottom:10px;">Top viewed</h3>
      <table class="data-table" style="margin-bottom:24px;">
        <thead><tr><th>Title</th><th>Views</th><th>Likes</th></tr></thead>
        <tbody>${stats.topViewed.map((a) => `<tr><td>${a.title}</td><td>${a.viewCount}</td><td>${a.likeCount}</td></tr>`).join("") || '<tr><td colspan="3">No data yet</td></tr>'}</tbody>
      </table>
      <h3 style="margin-bottom:10px;">Category breakdown</h3>
      <table class="data-table">
        <thead><tr><th>Category</th><th>Published articles</th></tr></thead>
        <tbody>${stats.categoryBreakdown.map((c) => `<tr><td>${c.categoryName}</td><td>${c.publishedCount}</td></tr>`).join("") || '<tr><td colspan="2">No data yet</td></tr>'}</tbody>
      </table>
    `;
  } catch (err) {
    document.getElementById("stat-grid").innerHTML = `<p class="form-error">${err.message}</p>`;
  }
}

// ---- Articles ----
let quill;
function initQuill() {
  if (quill) return;
  quill = new Quill("#content-editor", {
    theme: "snow",
    modules: { toolbar: [["bold", "italic", "underline"], [{ header: [2, 3, false] }], ["link", "image", "blockquote"], [{ list: "ordered" }, { list: "bullet" }], ["clean"]] }
  });
}

let categoriesCache = [];
async function loadCategoriesForSelect() {
  categoriesCache = await apiRequest("/api/categories");
  document.getElementById("article-category").innerHTML = categoriesCache
    .map((c) => `<option value="${c.slug}">${c.name}</option>`).join("");
}

const articleForm = document.getElementById("article-form");
let slugManuallyEdited = false;
document.getElementById("article-slug").addEventListener("input", () => { slugManuallyEdited = true; });
document.getElementById("article-title").addEventListener("input", (e) => {
  if (slugManuallyEdited) return;
  document.getElementById("article-slug").value = slugify(e.target.value);
});
function slugify(text) {
  return text.toLowerCase().trim().replace(/[^\w\s-]/g, "").replace(/[\s_-]+/g, "-").replace(/^-+|-+$/g, "");
}

document.getElementById("new-article-btn").addEventListener("click", () => {
  articleForm.reset();
  articleForm.id.value = "";
  slugManuallyEdited = false;
  quill.setContents([]);
  articleForm.classList.add("open");
  articleForm.scrollIntoView({ behavior: "smooth" });
});
document.getElementById("cancel-article-btn").addEventListener("click", () => articleForm.classList.remove("open"));

articleForm.addEventListener("submit", async (e) => {
  e.preventDefault();
  const errorEl = document.getElementById("article-form-error");
  errorEl.textContent = "";
  const formData = new FormData(articleForm);
  const id = formData.get("id");

  const payload = {
    title: formData.get("title"),
    subtitle: formData.get("subtitle") || null,
    content: quill.root.innerHTML,
    slug: formData.get("slug"),
    category: formData.get("category"),
    status: formData.get("status"),
    publishDate: formData.get("publishDate") ? formData.get("publishDate") + ":00" : null,
    source: formData.get("source") || null,
    videoUrl: formData.get("videoUrl") || null,
    tagNames: (formData.get("tags") || "").split(",").map((t) => t.trim()).filter(Boolean),
    imageUrl: formData.get("imageUrl") || null,
    imageAlt: formData.get("imageAlt") || null,
    imageSource: formData.get("imageSource") || null,
    imageCaption: formData.get("imageCaption") || null,
    seoTitle: formData.get("seoTitle") || null,
    seoDescription: formData.get("seoDescription") || null,
    featured: formData.get("featured") === "on"
  };

  try {
    if (id) {
      await apiRequest("/api/news/" + id, { method: "PUT", body: payload });
    } else {
      await apiRequest("/api/news", { method: "POST", body: payload });
    }
    articleForm.classList.remove("open");
    loadArticles();
    loadDashboard();
  } catch (err) {
    errorEl.textContent = err.message || "Could not save article.";
  }
});

async function loadArticles() {
  const wrap = document.getElementById("articles-table-wrap");
  wrap.innerHTML = `<p style="color:var(--text-muted)">Loading...</p>`;
  try {
    const result = await apiRequest("/api/news?page=0&size=100");
    const articles = result.content || [];
    if (!articles.length) {
      wrap.innerHTML = `<div class="empty-state">No articles yet. Create your first one above.</div>`;
      return;
    }
    wrap.innerHTML = `
      <table class="data-table">
        <thead><tr><th>Title</th><th>Category</th><th>Status</th><th>Views</th><th></th></tr></thead>
        <tbody>
          ${articles.map((a) => `
            <tr>
              <td>${a.title}${a.featured ? ' <span class="badge published">featured</span>' : ""}</td>
              <td>${a.category || ""}</td>
              <td>${statusBadge(a.status)}</td>
              <td>${a.viewCount}</td>
              <td class="table-actions">
                <button class="btn btn--ghost btn--sm" data-edit="${a.id}">Edit</button>
                <a class="btn btn--ghost btn--sm" href="/article/${a.slug}" target="_blank">View</a>
                <button class="btn btn--danger btn--sm" data-delete="${a.id}">Delete</button>
              </td>
            </tr>
          `).join("")}
        </tbody>
      </table>
    `;

    wrap.querySelectorAll("[data-edit]").forEach((btn) => {
      btn.addEventListener("click", async () => {
        const article = articles.find((a) => String(a.id) === btn.dataset.edit);
        fillArticleForm(article);
      });
    });
    wrap.querySelectorAll("[data-delete]").forEach((btn) => {
      btn.addEventListener("click", async () => {
        if (!confirm("Delete this article?")) return;
        try {
          await apiRequest("/api/news/" + btn.dataset.delete, { method: "DELETE" });
          loadArticles();
          loadDashboard();
        } catch (err) {
          alert(err.message || "Could not delete article.");
        }
      });
    });
  } catch (err) {
    wrap.innerHTML = `<div class="empty-state">Could not load articles.</div>`;
  }
}

function fillArticleForm(a) {
  articleForm.id.value = a.id;
  articleForm.title.value = a.title;
  articleForm.subtitle.value = a.subtitle || "";
  articleForm.slug.value = a.slug;
  slugManuallyEdited = true;
  articleForm.category.value = categoriesCache.find((c) => c.name === a.category)?.slug || "";
  articleForm.status.value = a.status;
  articleForm.publishDate.value = a.publishDate ? a.publishDate.slice(0, 16) : "";
  articleForm.source.value = a.source || "";
  articleForm.videoUrl.value = a.videoUrl || "";
  articleForm.tags.value = (a.tagNames || []).join(", ");
  articleForm.imageUrl.value = a.imageUrl || "";
  articleForm.imageAlt.value = a.imageAlt || "";
  articleForm.imageSource.value = a.imageSource || "";
  articleForm.imageCaption.value = a.imageCaption || "";
  articleForm.seoTitle.value = a.seoTitle || "";
  articleForm.seoDescription.value = a.seoDescription || "";
  document.getElementById("article-featured").checked = !!a.featured;
  quill.root.innerHTML = a.content || "";
  articleForm.classList.add("open");
  articleForm.scrollIntoView({ behavior: "smooth" });
}

// ---- Comments ----
async function loadComments() {
  const wrap = document.getElementById("comments-table-wrap");
  wrap.innerHTML = `<p style="color:var(--text-muted)">Loading...</p>`;
  try {
    const comments = await apiRequest("/api/admin/comments");
    if (!comments.length) {
      wrap.innerHTML = `<div class="empty-state">No comments yet.</div>`;
      return;
    }
    wrap.innerHTML = `
      <table class="data-table">
        <thead><tr><th>Article</th><th>Author</th><th>Comment</th><th>Status</th><th></th></tr></thead>
        <tbody>
          ${comments.map((c) => `
            <tr>
              <td>${c.articleTitle}</td>
              <td>${c.author}</td>
              <td>${c.content}</td>
              <td>${statusBadge(c.status)}</td>
              <td class="table-actions">
                <button class="btn btn--ghost btn--sm" data-approve="${c.id}" ${c.status === "approved" ? "disabled" : ""}>Approve</button>
                <button class="btn btn--danger btn--sm" data-delete-comment="${c.id}">Delete</button>
              </td>
            </tr>
          `).join("")}
        </tbody>
      </table>
    `;
    wrap.querySelectorAll("[data-approve]").forEach((btn) => {
      btn.addEventListener("click", async () => {
        await apiRequest("/api/admin/comments/" + btn.dataset.approve + "/approve", { method: "PATCH" });
        loadComments();
      });
    });
    wrap.querySelectorAll("[data-delete-comment]").forEach((btn) => {
      btn.addEventListener("click", async () => {
        if (!confirm("Delete this comment?")) return;
        await apiRequest("/api/admin/comments/" + btn.dataset.deleteComment, { method: "DELETE" });
        loadComments();
      });
    });
  } catch (err) {
    wrap.innerHTML = `<div class="empty-state">Could not load comments.</div>`;
  }
}

// ---- Categories ----
document.getElementById("category-form").addEventListener("submit", async (e) => {
  e.preventDefault();
  const errorEl = document.getElementById("category-form-error");
  errorEl.textContent = "";
  const formData = new FormData(e.target);
  try {
    await apiRequest("/api/categories", { method: "POST", body: { name: formData.get("name"), slug: formData.get("slug"), status: "active" } });
    e.target.reset();
    loadCategories();
    loadCategoriesForSelect();
  } catch (err) {
    errorEl.textContent = err.message || "Could not create category.";
  }
});

async function loadCategories() {
  const wrap = document.getElementById("categories-table-wrap");
  wrap.innerHTML = `<p style="color:var(--text-muted)">Loading...</p>`;
  try {
    const categories = await apiRequest("/api/categories");
    wrap.innerHTML = `
      <table class="data-table">
        <thead><tr><th>Name</th><th>Slug</th><th>Status</th></tr></thead>
        <tbody>${categories.map((c) => `<tr><td>${c.name}</td><td>${c.slug}</td><td>${c.status}</td></tr>`).join("")}</tbody>
      </table>
    `;
  } catch (err) {
    wrap.innerHTML = `<div class="empty-state">Could not load categories.</div>`;
  }
}

// ---- Media library ----
document.getElementById("media-upload-btn").addEventListener("click", () => {
  document.getElementById("media-file-input").click();
});
document.getElementById("media-file-input").addEventListener("change", async (e) => {
  const errorEl = document.getElementById("media-form-error");
  errorEl.textContent = "";
  const files = Array.from(e.target.files || []);
  for (const file of files) {
    const formData = new FormData();
    formData.append("file", file);
    try {
      await apiRequest("/api/media/upload", { method: "POST", body: formData, isForm: true });
    } catch (err) {
      errorEl.textContent = "Some files failed to upload: " + (err.message || "");
    }
  }
  e.target.value = "";
  loadMedia();
});

let mediaCache = [];
let pickingImageForArticle = false;

async function loadMedia() {
  const grid = document.getElementById("media-grid");
  try {
    mediaCache = await apiRequest("/api/media");
    if (!mediaCache.length) {
      grid.innerHTML = `<div class="empty-state">No uploaded images yet.</div>`;
      return;
    }
    grid.innerHTML = mediaCache.map((m) => `
      <div class="media-item">
        <img src="${m.fileUrl}" alt="${m.fileName}" data-use="${m.fileUrl}" style="cursor:pointer;" title="Click to use this image">
        <div class="media-item__url">${m.fileUrl}</div>
        <label style="display:flex; align-items:center; gap:6px; font-size:0.82rem; margin-top:6px;">
          <input type="checkbox" data-gallery-toggle="${m.id}" ${m.showInGallery ? "checked" : ""} /> গ্যালারিতে দেখান
        </label>
        <input type="text" class="media-caption-input" data-gallery-caption="${m.id}" placeholder="ক্যাপশন (ঐচ্ছিক)" value="${m.caption || ""}" style="width:100%; margin-top:4px; padding:6px 8px; font-size:0.82rem; border-radius:6px; border:1px solid var(--border, #ddd);" />
        <div class="media-item__actions">
          <button class="btn btn--ghost btn--sm" data-copy="${m.fileUrl}">Copy URL</button>
          <button class="btn btn--danger btn--sm" data-delete-media="${m.id}">Delete</button>
        </div>
      </div>
    `).join("");
    grid.querySelectorAll("[data-copy]").forEach((btn) => {
      btn.addEventListener("click", () => navigator.clipboard.writeText(btn.dataset.copy));
    });
    grid.querySelectorAll("[data-use]").forEach((img) => {
      img.addEventListener("click", () => {
        if (!pickingImageForArticle) return;
        document.getElementById("article-image-url").value = img.dataset.use;
        pickingImageForArticle = false;
        document.querySelector('[data-tab="articles"]').click();
      });
    });
    grid.querySelectorAll("[data-delete-media]").forEach((btn) => {
      btn.addEventListener("click", async () => {
        if (!confirm("Delete this image?")) return;
        await apiRequest("/api/media/" + btn.dataset.deleteMedia, { method: "DELETE" });
        loadMedia();
      });
    });
    grid.querySelectorAll("[data-gallery-toggle]").forEach((checkbox) => {
      checkbox.addEventListener("change", () => saveGallerySetting(checkbox.dataset.galleryToggle));
    });
    grid.querySelectorAll("[data-gallery-caption]").forEach((input) => {
      input.addEventListener("blur", () => saveGallerySetting(input.dataset.galleryCaption));
    });
  } catch (err) {
    grid.innerHTML = `<div class="empty-state">Could not load media.</div>`;
  }
}

async function saveGallerySetting(id) {
  const checkbox = document.querySelector(`[data-gallery-toggle="${id}"]`);
  const captionInput = document.querySelector(`[data-gallery-caption="${id}"]`);
  await apiRequest("/api/media/" + id + "/gallery", {
    method: "PATCH",
    body: { showInGallery: checkbox.checked, caption: captionInput.value },
  });
}

document.getElementById("pick-image-btn").addEventListener("click", () => {
  pickingImageForArticle = true;
  document.querySelector('[data-tab="media"]').click();
});

if (isLoggedIn()) {
  showDashboard();
}
