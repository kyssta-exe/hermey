#!/usr/bin/env python3
"""Self-check for the HERMEY gateway contract assumptions baked into the app.

Verifies against the LIVE server (read-only):
  1. POST /auth/password-login returns {"ok": true} + session cookies
  2. GET  /api/auth/me works with those cookies
  3. GET  /api/sessions rows carry the fields the app's SessionSummary maps
  4. GET  /api/cron/jobs is a bare array with id/paused_at/schedule{}
  5. GET  /api/skills is a bare array with name/description/enabled

Run: python3 test_gateway_contract.py   (needs /root/.hermes/.env basic creds)
"""
import json, os, sys
import requests

BASE = os.environ.get("HERMEY_TEST_URL", "https://hermes.ammar.click")
env = dict(
    line.strip().split("=", 1)
    for line in open("/root/.hermes/.env")
    if "=" in line and not line.startswith("#")
)

def check(name, cond, extra=""):
    print(("PASS" if cond else "FAIL"), name, extra)
    return cond

ok = True
s = requests.Session()

r = s.post(f"{BASE}/auth/password-login", json={
    "provider": "basic",
    "username": env["HERMES_DASHBOARD_BASIC_AUTH_USERNAME"],
    "password": env["HERMES_DASHBOARD_BASIC_AUTH_PASSWORD"],
}, timeout=15)
body = r.json()
ok &= check("login ok:true", r.status_code == 200 and body.get("ok") is True)
ok &= check("session cookie set", any("hermes_session" in c.name for c in s.cookies))

r = s.get(f"{BASE}/api/auth/me", timeout=15)
ok &= check("auth/me 200 + provider", r.status_code == 200 and r.json().get("provider") == "basic")

r = s.get(f"{BASE}/api/sessions?limit=5", timeout=15)
rows = r.json()["sessions"]
fields = {"id", "title", "message_count", "last_active"}
ok &= check("sessions rows have mapped fields", bool(rows) and fields <= set(rows[0]))

r = s.get(f"{BASE}/api/cron/jobs", timeout=15)
jobs = r.json()
jf = {"id", "name", "paused_at", "schedule", "enabled"}
ok &= check("cron jobs bare-array fields", isinstance(jobs, list) and jf <= set(jobs[0]))

r = s.get(f"{BASE}/api/skills", timeout=15)
skills = r.json()
sf = {"name", "description", "enabled"}
ok &= check("skills bare-array fields", isinstance(skills, list) and sf <= set(skills[0]))

sys.exit(0 if ok else 1)
