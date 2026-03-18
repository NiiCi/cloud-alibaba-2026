FROM elasticsearch:8.11.4

# macOS M 系列（ARM64）构建时需禁用 SVE 指令集，防止 JVM SIGILL 崩溃
ENV _JAVA_OPTIONS="-XX:UseSVE=0"

# 安装 IK 中文分词插件（版本必须与 ES 一致）
RUN bin/elasticsearch-plugin install --batch \
    https://release.infinilabs.com/analysis-ik/stable/elasticsearch-analysis-ik-8.11.4.zip
