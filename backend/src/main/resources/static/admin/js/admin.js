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
  initAboutBioQuill();
  loadDashboard();
  loadCategoriesForSelect();
  loadArticles();
  loadComments();
  loadCvRequests();
  loadPress();
  loadCategories();
  loadMedia();
  loadSiteInfo();
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

// Press clippings carry title/summary text scraped from other people's sites,
// so anything from that path is escaped before it reaches innerHTML.
function escapeHtml(value) {
  if (value === null || value === undefined) return "";
  return String(value)
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#39;");
}

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
      <div class="stat-card"><div class="value">${stats.pendingCvRequests}</div><div class="label">Pending CV requests</div></div>
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

// ---- CV requests ----
async function loadCvRequests() {
  const wrap = document.getElementById("cv-requests-table-wrap");
  wrap.innerHTML = `<p style="color:var(--text-muted)">Loading...</p>`;
  try {
    const requests = await apiRequest("/api/admin/cv-requests");
    if (!requests.length) {
      wrap.innerHTML = `<div class="empty-state">No CV requests yet.</div>`;
      return;
    }
    wrap.innerHTML = `
      <table class="data-table">
        <thead><tr><th>Date</th><th>Name</th><th>Email</th><th>Purpose</th><th>Status</th><th></th></tr></thead>
        <tbody>
          ${requests.map((r) => `
            <tr>
              <td>${(r.createdAt || "").slice(0, 16).replace("T", " ")}</td>
              <td>${r.name}</td>
              <td><a href="mailto:${r.email}?subject=${encodeURIComponent("সিভি — শাহজাদা আল হাবীব")}">${r.email}</a></td>
              <td>${r.purpose}</td>
              <td>${statusBadge(r.status)}</td>
              <td class="table-actions">
                <button class="btn btn--ghost btn--sm" data-cv-status="sent" data-cv-id="${r.id}" ${r.status === "sent" ? "disabled" : ""}>Mark sent</button>
                <button class="btn btn--ghost btn--sm" data-cv-status="declined" data-cv-id="${r.id}" ${r.status === "declined" ? "disabled" : ""}>Decline</button>
                <button class="btn btn--danger btn--sm" data-cv-delete="${r.id}">Delete</button>
              </td>
            </tr>
          `).join("")}
        </tbody>
      </table>
    `;
    wrap.querySelectorAll("[data-cv-status]").forEach((btn) => {
      btn.addEventListener("click", async () => {
        try {
          await apiRequest("/api/admin/cv-requests/" + btn.dataset.cvId + "/status", {
            method: "PATCH",
            body: { status: btn.dataset.cvStatus },
          });
          loadCvRequests();
          loadDashboard();
        } catch (err) {
          alert(err.message || "Could not update request.");
        }
      });
    });
    wrap.querySelectorAll("[data-cv-delete]").forEach((btn) => {
      btn.addEventListener("click", async () => {
        if (!confirm("Delete this CV request?")) return;
        await apiRequest("/api/admin/cv-requests/" + btn.dataset.cvDelete, { method: "DELETE" });
        loadCvRequests();
        loadDashboard();
      });
    });
  } catch (err) {
    wrap.innerHTML = `<div class="empty-state">Could not load CV requests.</div>`;
  }
}

document.getElementById("cv-file-download-btn").addEventListener("click", async () => {
  try {
    const token = localStorage.getItem("adminToken");
    const res = await fetch("/api/admin/cv-requests/file", {
      headers: token ? { Authorization: "Bearer " + token } : {},
    });
    if (!res.ok) throw new Error("Download failed (" + res.status + ")");
    const blob = await res.blob();
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = "cv.pdf";
    document.body.appendChild(a);
    a.click();
    a.remove();
    URL.revokeObjectURL(url);
  } catch (err) {
    alert(err.message || "Could not download the CV file.");
  }
});

// ---- Categories ----
const categoryForm = document.getElementById("category-form");

function resetCategoryForm() {
  categoryForm.reset();
  categoryForm.id.value = "";
  document.getElementById("category-submit-btn").textContent = "ক্যাটাগরি যোগ করুন";
  document.getElementById("category-cancel-btn").style.display = "none";
  document.getElementById("category-form-error").textContent = "";
}
document.getElementById("category-cancel-btn").addEventListener("click", resetCategoryForm);

categoryForm.addEventListener("submit", async (e) => {
  e.preventDefault();
  const errorEl = document.getElementById("category-form-error");
  errorEl.textContent = "";
  const formData = new FormData(e.target);
  const id = formData.get("id");
  const payload = { name: formData.get("name"), slug: formData.get("slug"), status: formData.get("status") || "active" };
  try {
    if (id) {
      await apiRequest("/api/categories/" + id, { method: "PUT", body: payload });
    } else {
      await apiRequest("/api/categories", { method: "POST", body: payload });
    }
    resetCategoryForm();
    loadCategories();
    loadCategoriesForSelect();
  } catch (err) {
    errorEl.textContent = err.message || "সংরক্ষণ করা গেল না।";
  }
});

let categoriesTableCache = [];
async function loadCategories() {
  const wrap = document.getElementById("categories-table-wrap");
  wrap.innerHTML = `<p style="color:var(--text-muted)">Loading...</p>`;
  try {
    categoriesTableCache = await apiRequest("/api/categories");
    wrap.innerHTML = `
      <table class="data-table">
        <thead><tr><th>Name</th><th>Slug</th><th>Status</th><th></th></tr></thead>
        <tbody>${categoriesTableCache.map((c) => `
          <tr>
            <td>${escapeHtml(c.name)}</td>
            <td>${escapeHtml(c.slug)}</td>
            <td>${statusBadge(c.status)}</td>
            <td class="table-actions">
              <button class="btn btn--ghost btn--sm" data-category-edit="${c.id}">সম্পাদনা</button>
              <button class="btn btn--danger btn--sm" data-category-delete="${c.id}">মুছুন</button>
            </td>
          </tr>
        `).join("")}</tbody>
      </table>
    `;
    wrap.querySelectorAll("[data-category-edit]").forEach((btn) => {
      btn.addEventListener("click", () => {
        const c = categoriesTableCache.find((x) => String(x.id) === btn.dataset.categoryEdit);
        if (!c) return;
        categoryForm.id.value = c.id;
        categoryForm.name.value = c.name || "";
        categoryForm.slug.value = c.slug || "";
        categoryForm.status.value = c.status || "active";
        document.getElementById("category-submit-btn").textContent = "সংরক্ষণ করুন";
        document.getElementById("category-cancel-btn").style.display = "inline-flex";
        categoryForm.scrollIntoView({ behavior: "smooth" });
      });
    });
    wrap.querySelectorAll("[data-category-delete]").forEach((btn) => {
      btn.addEventListener("click", async () => {
        if (!confirm("এই ক্যাটাগরিটি মুছে ফেলবেন?")) return;
        try {
          await apiRequest("/api/categories/" + btn.dataset.categoryDelete, { method: "DELETE" });
          loadCategories();
          loadCategoriesForSelect();
        } catch (err) {
          alert(err.message || "মুছে ফেলা গেল না।");
        }
      });
    });
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
// Which form + field a media click should fill, and which tab to return to.
// Generalized so both the article form's image field and the site-info form's
// profile/book-cover fields can reuse the same "pick from library" flow.
let pickImageTarget = null;

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
        if (!pickImageTarget) return;
        const field = document.getElementById(pickImageTarget.formId).querySelector(`[name="${pickImageTarget.fieldName}"]`);
        if (field) field.value = img.dataset.use;
        const returnTab = pickImageTarget.returnTab;
        pickImageTarget = null;
        document.querySelector(`[data-tab="${returnTab}"]`).click();
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
  pickImageTarget = { formId: "article-form", fieldName: "imageUrl", returnTab: "articles" };
  document.querySelector('[data-tab="media"]').click();
});

// Same picker, wired to whichever site-info field the clicked button names.
document.querySelectorAll("[data-pick-image]").forEach((btn) => {
  btn.addEventListener("click", () => {
    pickImageTarget = { formId: "site-info-form", fieldName: btn.dataset.pickImage, returnTab: "site-info" };
    document.querySelector('[data-tab="media"]').click();
  });
});

// ---- Change password ----
document.getElementById("change-password-form").addEventListener("submit", async (e) => {
  e.preventDefault();
  const form = e.target;
  const errorEl = document.getElementById("change-password-error");
  const successEl = document.getElementById("change-password-success");
  errorEl.textContent = "";
  successEl.style.display = "none";

  const currentPassword = form.currentPassword.value;
  const newPassword = form.newPassword.value;
  const confirmPassword = form.confirmPassword.value;

  if (newPassword !== confirmPassword) {
    errorEl.textContent = "নতুন পাসওয়ার্ড দুটো মিলছে না।";
    return;
  }

  try {
    await apiRequest("/api/admin/profile/password", {
      method: "PATCH",
      body: { currentPassword, newPassword },
    });
    successEl.style.display = "block";
    form.reset();
  } catch (err) {
    errorEl.textContent = err.message || "পাসওয়ার্ড পরিবর্তন করা যায়নি।";
  }
});

// ---- site info (author profile, book, socials) ----
let aboutBioQuill;
function initAboutBioQuill() {
  if (aboutBioQuill) return;
  aboutBioQuill = new Quill("#about-bio-editor", {
    theme: "snow",
    modules: { toolbar: [["bold", "italic", "underline"], ["link"], [{ list: "bullet" }], ["clean"]] },
  });
}

const siteInfoForm = document.getElementById("site-info-form");

async function loadSiteInfo() {
  const errorEl = document.getElementById("site-info-error");
  try {
    const settings = await apiRequest("/api/admin/settings");
    for (const [name, value] of Object.entries(settings)) {
      const field = siteInfoForm.querySelector(`[name="${name}"]`);
      if (!field) continue;
      if (field.type === "checkbox") field.checked = !!value;
      else field.value = value || "";
    }
    aboutBioQuill.root.innerHTML = settings.aboutBio || "";
  } catch (err) {
    errorEl.textContent = err.message || "সাইট তথ্য লোড করা গেল না।";
  }
}

siteInfoForm.addEventListener("submit", async (e) => {
  e.preventDefault();
  const errorEl = document.getElementById("site-info-error");
  const successEl = document.getElementById("site-info-success");
  errorEl.textContent = "";
  successEl.style.display = "none";

  const fd = new FormData(siteInfoForm);
  const payload = {};
  for (const [name, value] of fd.entries()) payload[name] = value;
  // FormData omits unchecked boxes entirely and sends "on" for checked ones —
  // read the real booleans straight from the inputs instead.
  siteInfoForm.querySelectorAll('input[type="checkbox"]').forEach((box) => {
    payload[box.name] = box.checked;
  });
  payload.aboutBio = aboutBioQuill.root.innerHTML;

  try {
    await apiRequest("/api/admin/settings", { method: "PUT", body: payload });
    successEl.style.display = "block";
    setTimeout(() => { successEl.style.display = "none"; }, 3000);
  } catch (err) {
    errorEl.textContent = err.message || "সংরক্ষণ করা গেল না।";
  }
});

if (isLoggedIn()) {
  showDashboard();
}

// ---- press clippings ----
const PRESS_KIND_LABELS = {
  feature: "ফিচার",
  report: "রিপোর্ট",
  literature: "সাহিত্য",
  column: "কলাম/মতামত",
  interview: "সাক্ষাৎকার",
  travel: "ভ্রমণ",
  photography: "ফটোগ্রাফি",
};

const pressForm = document.getElementById("press-form");

function pressFormValues() {
  const fd = new FormData(pressForm);
  return {
    title: fd.get("title"),
    publication: fd.get("publication"),
    url: fd.get("url"),
    summary: fd.get("summary"),
    imageUrl: fd.get("imageUrl"),
    publishedOn: fd.get("publishedOn") || null,
    kind: fd.get("kind") || "feature",
    status: fd.get("status") || "active",
  };
}

function resetPressForm() {
  pressForm.reset();
  pressForm.querySelector("[name=id]").value = "";
  document.getElementById("press-form-error").textContent = "";
  document.getElementById("press-preview-note").textContent = "";
}

// Read the linked page's Open Graph tags and fill the blank fields. Runs on its
// own when you leave the URL box (auto === true) and on the button (auto === false).
let pressPreviewedUrl = "";
async function runPressPreview(auto) {
  const note = document.getElementById("press-preview-note");
  const errorEl = document.getElementById("press-form-error");
  const url = pressForm.querySelector("[name=url]").value.trim();

  if (!/^https?:\/\/\S+\.\S+/.test(url)) {
    if (!auto) errorEl.textContent = "আগে একটি সঠিক লিংক দিন।";
    return;
  }
  // Don't re-fetch the same URL every time the box loses focus.
  if (auto && url === pressPreviewedUrl) return;
  pressPreviewedUrl = url;

  errorEl.textContent = "";
  note.textContent = "লিংক পড়া হচ্ছে...";
  try {
    const preview = await apiRequest("/api/admin/press/preview", { method: "POST", body: { url } });
    const setIfEmpty = (name, value) => {
      const field = pressForm.querySelector("[name=" + name + "]");
      if (field && value && !field.value.trim()) field.value = value;
    };
    // Only fills blanks, so a correction you typed is never overwritten.
    setIfEmpty("title", preview.title);
    setIfEmpty("publication", preview.publication);
    setIfEmpty("summary", preview.summary);
    setIfEmpty("imageUrl", preview.imageUrl);
    setIfEmpty("publishedOn", preview.publishedOn);
    const gotImage = !!(preview.imageUrl && preview.imageUrl.trim());
    note.textContent = gotImage
      ? "তথ্য এসেছে (ছবিসহ) — দরকার হলে ঠিক করে সংরক্ষণ করুন।"
      : "তথ্য এসেছে, তবে এই সাইট থেকে ছবি পাওয়া যায়নি — চাইলে ছবির লিংক হাতে দিন।";
  } catch (err) {
    note.textContent = "";
    // A failed auto-fetch stays quiet so you can just keep typing by hand.
    if (!auto) errorEl.textContent = err.message || "লিংকটি পড়া গেল না — তথ্যগুলো হাতে লিখে দিন।";
    else note.textContent = "লিংক থেকে তথ্য আনা গেল না — হাতে লিখে দিন।";
    pressPreviewedUrl = "";
  }
}

document.getElementById("press-fetch-btn").addEventListener("click", () => runPressPreview(false));
// 'change' fires when the field loses focus after an edit — the natural "I've
// pasted the link" moment, so the preview just happens.
pressForm.querySelector("[name=url]").addEventListener("change", () => runPressPreview(true));

pressForm.addEventListener("submit", async (e) => {
  e.preventDefault();
  const errorEl = document.getElementById("press-form-error");
  errorEl.textContent = "";
  const id = pressForm.querySelector("[name=id]").value;
  try {
    await apiRequest("/api/admin/press" + (id ? "/" + id : ""), {
      method: id ? "PUT" : "POST",
      body: pressFormValues(),
    });
    resetPressForm();
    loadPress();
  } catch (err) {
    errorEl.textContent = err.message || "সংরক্ষণ করা গেল না।";
  }
});

document.getElementById("press-cancel-btn").addEventListener("click", resetPressForm);

async function loadPress() {
  const wrap = document.getElementById("press-table-wrap");
  wrap.innerHTML = `<p style="color:var(--text-muted)">Loading...</p>`;
  try {
    const items = await apiRequest("/api/admin/press");
    if (!items.length) {
      wrap.innerHTML = `<div class="empty-state">এখনো কোনো লিংক যোগ করা হয়নি।</div>`;
      return;
    }
    wrap.innerHTML = `
      <table class="data-table">
        <thead><tr><th>তারিখ</th><th>পত্রিকা</th><th>ধরন</th><th>শিরোনাম</th><th>অবস্থা</th><th></th></tr></thead>
        <tbody>
          ${items.map((p) => `
            <tr>
              <td>${escapeHtml(p.publishedOn || "—")}</td>
              <td>${escapeHtml(p.publication)}</td>
              <td>${escapeHtml(PRESS_KIND_LABELS[p.kind] || p.kind)}</td>
              <td><a href="${encodeURI(p.url)}" target="_blank" rel="noopener">${escapeHtml(p.title)}</a></td>
              <td>${statusBadge(p.status)}</td>
              <td class="table-actions">
                <button class="btn btn--ghost btn--sm" data-press-edit="${p.id}">সম্পাদনা</button>
                <button class="btn btn--danger btn--sm" data-press-delete="${p.id}">মুছুন</button>
              </td>
            </tr>
          `).join("")}
        </tbody>
      </table>
    `;
    wrap.querySelectorAll("[data-press-edit]").forEach((btn) => {
      btn.addEventListener("click", () => {
        const item = items.find((p) => String(p.id) === btn.dataset.pressEdit);
        if (!item) return;
        pressForm.querySelector("[name=id]").value = item.id;
        pressForm.querySelector("[name=title]").value = item.title || "";
        pressForm.querySelector("[name=publication]").value = item.publication || "";
        pressForm.querySelector("[name=url]").value = item.url || "";
        pressForm.querySelector("[name=summary]").value = item.summary || "";
        pressForm.querySelector("[name=imageUrl]").value = item.imageUrl || "";
        pressForm.querySelector("[name=publishedOn]").value = item.publishedOn || "";
        pressForm.querySelector("[name=kind]").value = item.kind || "feature";
        pressForm.querySelector("[name=status]").value = item.status || "active";
        pressForm.scrollIntoView({ behavior: "smooth" });
      });
    });
    wrap.querySelectorAll("[data-press-delete]").forEach((btn) => {
      btn.addEventListener("click", async () => {
        if (!confirm("এই লিংকটি মুছে ফেলবেন?")) return;
        await apiRequest("/api/admin/press/" + btn.dataset.pressDelete, { method: "DELETE" });
        loadPress();
      });
    });
  } catch (err) {
    wrap.innerHTML = `<p class="form-error">${escapeHtml(err.message || "লোড করা গেল না।")}</p>`;
  }
}

