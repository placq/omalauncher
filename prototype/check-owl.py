#!/usr/bin/env python3
"""Check the language changes on ARM Linux without configuring the Android SDK."""
import concurrent.futures
import pathlib
import subprocess
import sys
import urllib.request

root = pathlib.Path(__file__).resolve().parents[1]
out = root / "prototype" / "build"
deps = out / "deps"
deps.mkdir(parents=True, exist_ok=True)
artifacts = [
    ("org.jetbrains.kotlin", "kotlin-compiler-embeddable", "2.4.20"),
    ("org.jetbrains.kotlin", "kotlin-build-tools-api", "2.4.20"),
    ("org.jetbrains.kotlin", "kotlin-stdlib", "2.4.20"),
    ("org.jetbrains.kotlin", "kotlin-script-runtime", "2.4.20"),
    ("org.jetbrains.kotlin", "kotlin-reflect", "1.6.10"),
    ("org.jetbrains.kotlin", "kotlin-daemon-embeddable", "2.4.20"),
    ("org.jetbrains.kotlinx", "kotlinx-coroutines-core-jvm", "1.8.0"),
    ("org.jetbrains", "annotations", "13.0"),
    ("junit", "junit", "4.13.2"),
    ("org.hamcrest", "hamcrest-core", "1.3"),
]


def download(artifact):
    group, name, version = artifact
    jar = deps / f"{name}-{version}.jar"
    if not jar.exists():
        print(f"Downloading {jar.name}", flush=True)
        url = f"https://repo.maven.apache.org/maven2/{group.replace('.', '/')}/{name}/{version}/{jar.name}"
        with urllib.request.urlopen(url, timeout=90) as response:
            jar.write_bytes(response.read())
    return jar


with concurrent.futures.ThreadPoolExecutor(max_workers=4) as pool:
    jars = list(pool.map(download, artifacts))
classpath = ":".join(map(str, jars))
classes = out / "classes"
tests = out / "tests"


def compile_sources(paths, target, extra=""):
    print(f"Compiling {target.name}", flush=True)
    sources = sorted(str(p) for folder in paths for p in (root / folder).rglob("*.kt"))
    friend = [f"-Xfriend-paths={classes}"] if extra else []
    subprocess.run([
        "java", "-Xmx1536m", "-cp", classpath,
        "org.jetbrains.kotlin.cli.jvm.K2JVMCompiler",
        "-no-stdlib", "-no-reflect", "-jvm-target", "17",
        "-classpath", classpath + extra, "-d", str(target), *friend, *sources,
    ], check=True, cwd=root)


if "--skip-compile" not in sys.argv:
    compile_sources(["owl/src/main/kotlin", "owl/src/cli/kotlin"], classes)
    compile_sources(["owl/src/test/kotlin"], tests, ":" + str(classes))
test_classes = [
    "dev.omalauncher.widgets.lang.LangTest",
    "dev.omalauncher.widgets.lang.GbaTest",
    "dev.omalauncher.widgets.lang.AppSearchTest",
    "dev.omalauncher.widgets.market.ShareTest",
    "dev.omalauncher.widgets.market.MarketTest",
    "dev.omalauncher.widgets.market.SubmitTest",
]
runtime = f"{classes}:{tests}:{classpath}"
subprocess.run(["java", "-cp", runtime, "org.junit.runner.JUnitCore", *test_classes], check=True, cwd=root / "owl")
subprocess.run([
    "java", "-cp", runtime, "dev.omalauncher.widgets.cli.MainKt", "check",
    "prototype/pocket-gba.owl",
], check=True, cwd=root)
