#!/usr/bin/env bash
# 把 natives/ 目录里的 libproxy_*.so / *.dll 补进编译好的 TouchController 主 jar。
# 缺哪个补哪个，jar 里已有的自动跳过（重复跑也安全）。
#
# 用法:
#   ./merge_natives.sh <主jar路径> [natives目录，默认 ./natives]
#
# 单独验证:
#   ./merge_natives.sh /workspace/uploads/你的.jar
set -euo pipefail

JAR="${1:?用法: merge_natives.sh <jar文件> [natives目录]}"
NATIVES="${2:-natives}"

command -v zip >/dev/null || { echo "需要 zip 命令（GitHub Actions 的 ubuntu 自带）" >&2; exit 1; }
[ -f "$JAR" ] || { echo "找不到 jar: $JAR" >&2; exit 1; }
[ -d "$NATIVES" ] || { echo "找不到库目录: $NATIVES" >&2; exit 1; }

JAR_ABS="$(realpath "$JAR")"
NAT_ABS="$(realpath "$NATIVES")"

added=0
skipped=0
while IFS= read -r f; do
  if unzip -l "$JAR_ABS" "$f" >/dev/null 2>&1; then
    skipped=$((skipped+1))
  else
    (cd "$NAT_ABS" && zip -q -X -D "$JAR_ABS" "$f")
    echo "补入: $f"
    added=$((added+1))
  fi
done < <(cd "$NAT_ABS" && find . -type f \( -name '*.so' -o -name '*.dll' \) | sed 's|^\./||' | sort)

echo "完成：补入 $added 个，跳过已有 $skipped 个"
if [ $((added+skipped)) -ne 14 ]; then
  echo "警告：natives 目录里库文件总数是 $((added+skipped))，正常应为 14，请检查是否解压完整" >&2
  exit 1
fi
