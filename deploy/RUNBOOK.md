# Milestone 1 — get the site live on the GCP VM (HTTP only)

**Goal:** the Spring Boot app running in one Docker container on the e2-micro VM,
talking to the existing Aiven MySQL, reachable at `http://<VM_EXTERNAL_IP>`, with
a keep-alive pinger so Aiven never idles into a power-off.

**Not in this milestone:** a domain, HTTPS, Cloudflare, CI/CD. Those are M2+.

Render keeps running untouched as a free fallback the whole time.

---

## 0. Prerequisites (one-time)

### 0a. Open port 80 in the GCP firewall
In the GCP Console: **VPC network → Firewall → Create firewall rule**
- Targets: *All instances in the network* (simplest) or the VM's network tag
- Source IPv4 ranges: `0.0.0.0/0`
- Protocols/ports: TCP `80`

Or with the CLI:
```
gcloud compute firewall-rules create allow-http --allow tcp:80 --source-ranges 0.0.0.0/0
```

### 0b. Confirm Docker + Compose on the VM
```
docker --version && docker compose version
```
If `docker compose` is missing:
```
sudo apt-get update && sudo apt-get install -y docker-compose-plugin
```
If `docker` needs sudo every time, add yourself to the group (then log out/in):
```
sudo usermod -aG docker $USER
```

---

## 1. Add swap (required — 1 GB RAM cannot build the image on its own)
```
sudo fallocate -l 2G /swapfile
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab
free -h
```
`free -h` should now show ~2.0Gi of swap.

---

## 2. Clone the repo
```
cd ~
git clone https://github.com/shahajada-habib/Shahajada-Al-Habib-Website.git
cd Shahajada-Al-Habib-Website/deploy
```

---

## 3. Fill in the environment file
```
cp .env.example .env
nano .env
```
Fill every blank. Get `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET` from
the **Render dashboard → service → Environment** tab — copy them exactly.
Set `SITE_URL` and `CORS_ALLOWED_ORIGINS` to `http://<VM_EXTERNAL_IP>`.
Save: `Ctrl+O`, `Enter`, `Ctrl+X`.

---

## 4. Build and start
```
docker compose up -d --build
```
The first build runs Maven inside Docker and takes **5–10 minutes** on this VM.
Watch it come up:
```
docker compose logs -f web
```
Wait for a line like `Started BlogCmsApplication in N seconds`. `Ctrl+C` stops
following the logs (the container keeps running).

If it dies with an out-of-memory error during build: swap isn't active (redo
step 1) or fall back to **Appendix A** (build on your PC, copy the image over).

---

## 5. Verify
```
curl -s http://localhost/api/health
```
Expect: `{"status":"UP","db":"UP"}`

Then open `http://<VM_EXTERNAL_IP>` in a browser — the site should load, and
`http://<VM_EXTERNAL_IP>/admin/` should show the admin login.

---

## 6. Keep-alive pinger (stops Aiven from powering off)
1. Sign up free at <https://cron-job.org>.
2. **Create cronjob**:
   - URL: `http://<VM_EXTERNAL_IP>/api/health`
   - Schedule: every 5 minutes
   - Save.
3. It also has "notify on failure" — turn that on to get an email if the site
   ever goes down.

(After Milestone 2 gives you HTTPS, edit this job's URL to the `https://` one.)

---

## Day-to-day

| Task | Command (run from `~/Shahajada-Al-Habib-Website/deploy`) |
|------|------|
| Deploy latest code | `git pull && docker compose up -d --build` |
| View logs | `docker compose logs -f web` |
| Restart | `docker compose restart web` |
| Stop everything | `docker compose down` |
| Memory / CPU use | `docker stats --no-stream` |
| Disk used by Docker | `docker system df` |

---

## Appendix A — fallback: build on your PC, ship the image

If the VM can't build (out of memory), build where you have RAM:

On your PC, in the repo root:
```
docker build -t shahajada-site:local ./backend
docker save shahajada-site:local | gzip > site.tar.gz
scp site.tar.gz <user>@<VM_EXTERNAL_IP>:~/
```
On the VM:
```
gunzip -c ~/site.tar.gz | docker load
```
Then edit `deploy/docker-compose.yml`: comment out the `build:` block so it uses
the pre-loaded `image: shahajada-site:local`, and run `docker compose up -d`.
