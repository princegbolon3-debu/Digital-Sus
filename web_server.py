#!/usr/bin/env python3
"""
Digital Susu - Web Hub & APK Distribution Server
Listens on port 3000 (proxied by Nginx on port 8080).
Provides:
- Responsive Web Landing Page with App Overview & Live Documents (SRS, PRD, SDD, SSD)
- High-Performance Android APK File Streaming with full HTTP Range request support
- Direct Download endpoints (/download, /DigitalSusu.apk, /digital-susu.apk)
"""

import os
import sys
import mimetypes
import re
from http.server import HTTPServer, BaseHTTPRequestHandler
from socketserver import ThreadingMixIn

PORT = 3000
PUBLIC_DIR = os.path.abspath(os.path.join(os.path.dirname(__file__), "public"))
ROOT_DIR = os.path.abspath(os.path.dirname(__file__))

mimetypes.add_type("application/manifest+json", ".json")
mimetypes.add_type("application/javascript", ".js")
mimetypes.add_type("image/png", ".png")
mimetypes.add_type("text/html; charset=utf-8", ".html")

APK_FILENAME = "DigitalSusu.apk"
APK_PATH = os.path.join(PUBLIC_DIR, APK_FILENAME)
if not os.path.exists(APK_PATH):
    # fallback
    fallback = os.path.join(ROOT_DIR, "app/build/outputs/apk/debug/app-debug.apk")
    if os.path.exists(fallback):
        APK_PATH = fallback

DOC_FILES = {
    "srs": os.path.join(ROOT_DIR, "SRS_SOFTWARE_REQUIREMENTS_SPECIFICATION.md"),
    "prd": os.path.join(ROOT_DIR, "PRD_PRODUCT_REQUIREMENTS_DOCUMENT.md"),
    "sdd": os.path.join(ROOT_DIR, "SDD_SOFTWARE_DESIGN_DOCUMENT.md"),
    "ssd": os.path.join(ROOT_DIR, "SYSTEM_SEQUENCE_DIAGRAM_SSD.md"),
}


class ThreadedHTTPServer(ThreadingMixIn, HTTPServer):
    daemon_threads = True
    allow_reuse_address = True


class SusuRequestHandler(BaseHTTPRequestHandler):
    protocol_version = "HTTP/1.1"

    def do_HEAD(self):
        self.handle_request(send_body=False)

    def do_GET(self):
        self.handle_request(send_body=True)

    def handle_request(self, send_body=True):
        path = self.path.split("?")[0].rstrip("/")
        if path == "":
            path = "/"

        # Download routes
        if path in ["/download", "/DigitalSusu.apk", "/digital-susu.apk", "/app-debug.apk"]:
            self.serve_file(APK_PATH, content_type="application/vnd.android.package-archive",
                            download_filename="DigitalSusu.apk", send_body=send_body)
            return

        # Document JSON API endpoints
        doc_match = re.match(r"^/api/doc/(srs|prd|sdd|ssd)$", path)
        if doc_match:
            doc_key = doc_match.group(1)
            file_path = DOC_FILES.get(doc_key)
            if file_path and os.path.exists(file_path):
                with open(file_path, "r", encoding="utf-8") as f:
                    content = f.read()
                data = content.encode("utf-8")
                self.send_response(200)
                self.send_header("Content-Type", "text/plain; charset=utf-8")
                self.send_header("Content-Length", str(len(data)))
                self.send_header("Access-Control-Allow-Origin", "*")
                self.end_headers()
                if send_body:
                    self.wfile.write(data)
                return

        # Static files from public dir
        if path == "/":
            target = os.path.join(PUBLIC_DIR, "index.html")
        else:
            rel = path.lstrip("/")
            target = os.path.join(PUBLIC_DIR, rel)

        if os.path.exists(target) and os.path.isfile(target):
            mime_type, _ = mimetypes.guess_type(target)
            if not mime_type:
                mime_type = "application/octet-stream"
            download_name = "DigitalSusu.apk" if target.endswith(".apk") else None
            self.serve_file(target, content_type=mime_type, download_filename=download_name, send_body=send_body)
        else:
            # Fallback to index.html
            index_path = os.path.join(PUBLIC_DIR, "index.html")
            if os.path.exists(index_path):
                self.serve_file(index_path, content_type="text/html; charset=utf-8", send_body=send_body)
            else:
                self.send_error(404, "File Not Found")

    def serve_file(self, file_path, content_type="application/octet-stream", download_filename=None, send_body=True):
        try:
            file_size = os.path.getsize(file_path)
        except OSError:
            self.send_error(404, "File Not Found")
            return

        range_header = self.headers.get("Range")
        start = 0
        end = file_size - 1
        status_code = 200

        if range_header:
            m = re.match(r"bytes=(\d+)-(\d*)", range_header)
            if m:
                status_code = 206
                start = int(m.group(1))
                if m.group(2):
                    end = int(m.group(2))
                if start >= file_size or end >= file_size or start > end:
                    self.send_response(416)
                    self.send_header("Content-Range", f"bytes */{file_size}")
                    self.end_headers()
                    return

        content_length = (end - start) + 1

        self.send_response(status_code)
        self.send_header("Content-Type", content_type)
        self.send_header("Accept-Ranges", "bytes")
        self.send_header("Content-Length", str(content_length))
        if status_code == 206:
            self.send_header("Content-Range", f"bytes {start}-{end}/{file_size}")
        if download_filename:
            self.send_header("Content-Disposition", f'attachment; filename="{download_filename}"')
        self.send_header("Access-Control-Allow-Origin", "*")
        self.end_headers()

        if not send_body:
            return

        try:
            with open(file_path, "rb") as f:
                f.seek(start)
                remaining = content_length
                chunk_size = 64 * 1024  # 64KB chunks
                while remaining > 0:
                    read_len = min(remaining, chunk_size)
                    chunk = f.read(read_len)
                    if not chunk:
                        break
                    self.wfile.write(chunk)
                    remaining -= len(chunk)
        except (BrokenPipeError, ConnectionResetError):
            pass


def run():
    print(f"Starting Digital Susu Web Server on port {PORT}...")
    server = ThreadedHTTPServer(("0.0.0.0", PORT), SusuRequestHandler)
    server.serve_forever()


if __name__ == "__main__":
    run()
