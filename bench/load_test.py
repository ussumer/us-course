#!/usr/bin/env python3
"""Small, dependency-free HTTP load generator for the US Course demo.

It deliberately keeps the request and result format simple so a run can be
reproduced on a server without installing JMeter or another agent.
"""
import argparse
import concurrent.futures
import json
import os
import statistics
import sys
import threading
import time
import urllib.error
import urllib.request


def parse_args():
    p = argparse.ArgumentParser(description="Concurrent HTTP load test")
    p.add_argument("--url", required=True, help="complete endpoint URL")
    p.add_argument("--method", default="POST")
    p.add_argument("--count", type=int, required=True)
    p.add_argument("--concurrency", type=int, required=True)
    p.add_argument("--body-file", help="JSON body template; {index} and {user_id} are available")
    p.add_argument("--body", help="literal JSON body (used when --body-file is absent)")
    p.add_argument("--tokens-file", help="one access token per line; tokens are rotated")
    p.add_argument("--token", default=os.getenv("ACCESS_TOKEN", ""))
    p.add_argument("--user-id", default=os.getenv("USER_ID", ""))
    p.add_argument("--base-lesson-id", type=int, default=None)
    p.add_argument("--section-id", type=int, default=16)
    p.add_argument("--moment", type=int, default=56)
    p.add_argument("--timeout", type=float, default=15)
    p.add_argument("--output", default="load-result.json")
    return p.parse_args()


def percentile(values, q):
    if not values:
        return None
    values = sorted(values)
    pos = (len(values) - 1) * q
    lo, hi = int(pos), min(int(pos) + 1, len(values) - 1)
    return values[lo] + (values[hi] - values[lo]) * (pos - lo)


def main():
    args = parse_args()
    if args.count <= 0 or args.concurrency <= 0:
        raise SystemExit("--count and --concurrency must be positive")
    tokens = []
    if args.tokens_file:
        with open(args.tokens_file, encoding="utf-8") as f:
            tokens = [line.strip() for line in f if line.strip()]
    if not tokens and args.token:
        tokens = [args.token]
    body_template = None
    if args.body_file:
        with open(args.body_file, encoding="utf-8") as f:
            body_template = f.read()
    lock = threading.Lock()
    samples, errors, statuses = [], [], {}
    business_codes = {}

    def one(index):
        user_id = args.user_id or (str(index + 1) if tokens else "")
        body = None
        if body_template is not None:
            # JSON itself contains braces, so use explicit tokens rather than
            # str.format(), which would interpret every JSON object brace.
            lesson_id = "" if args.base_lesson_id is None else str(args.base_lesson_id + index % len(tokens))
            body = (body_template.replace("{index}", str(index))
                    .replace("{user_id}", user_id)
                    .replace("{lesson_id}", lesson_id)
                    .replace("{section_id}", str(args.section_id))
                    .replace("{moment}", str(args.moment))).encode()
        elif args.body is not None:
            body = args.body.encode()
        headers = {"Accept": "application/json"}
        if body is not None:
            headers["Content-Type"] = "application/json"
        if tokens:
            headers["Authorization"] = tokens[index % len(tokens)]
        request = urllib.request.Request(args.url, data=body, headers=headers, method=args.method.upper())
        start = time.perf_counter()
        status, error, business_code = None, None, None
        try:
            with urllib.request.urlopen(request, timeout=args.timeout) as response:
                payload = response.read()
                status = response.status
                try:
                    business_code = str(json.loads(payload)["code"])
                except (ValueError, KeyError, TypeError):
                    business_code = "invalid_response"
        except urllib.error.HTTPError as exc:
            status = exc.code
            business_code = "http_error"
            try:
                exc.read()
            except Exception:
                pass
        except Exception as exc:  # connection, timeout, DNS, etc.
            error = repr(exc)
        elapsed_ms = (time.perf_counter() - start) * 1000
        with lock:
            samples.append(elapsed_ms)
            if status is not None:
                statuses[str(status)] = statuses.get(str(status), 0) + 1
            if business_code is not None:
                business_codes[business_code] = business_codes.get(business_code, 0) + 1
            if error:
                errors.append(error)

    started = time.perf_counter()
    with concurrent.futures.ThreadPoolExecutor(max_workers=args.concurrency) as pool:
        list(pool.map(one, range(args.count)))
    elapsed = time.perf_counter() - started
    result = {
        "url": args.url,
        "method": args.method.upper(),
        "requests": args.count,
        "concurrency": args.concurrency,
        "elapsedSeconds": round(elapsed, 6),
        "qps": round(args.count / elapsed, 3) if elapsed else None,
        "statuses": statuses,
        "businessCodes": business_codes,
        "acceptedQps": round(business_codes.get("200", 0) / elapsed, 3),
        "transportErrors": len(errors),
        "latencyMs": {
            "min": round(min(samples), 3) if samples else None,
            "mean": round(statistics.mean(samples), 3) if samples else None,
            "p50": round(percentile(samples, .50), 3) if samples else None,
            "p95": round(percentile(samples, .95), 3) if samples else None,
            "p99": round(percentile(samples, .99), 3) if samples else None,
            "max": round(max(samples), 3) if samples else None,
        },
        "sampleErrors": errors[:10],
    }
    with open(args.output, "w", encoding="utf-8") as f:
        json.dump(result, f, ensure_ascii=False, indent=2)
    print(json.dumps(result, ensure_ascii=False, indent=2))
    return 0 if not errors else 2


if __name__ == "__main__":
    sys.exit(main())
