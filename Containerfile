FROM temurin:11-jdk-jammy

RUN curl -s https://api.github.com/repos/dkoboldt/varscan/releases/latest | grep browser_download_url | grep '.jar' | head -n 1 | cut -d '"' -f 4 | wget -qi - \
    && mv VarScan*jar VarScan.jar \
    && chmod +x VarScan.jar \
    && mkdir -p /opt/varscan \
    && mv VarScan.jar /opt/varscan/

COPY dist/*.jar /opt/genomephone/

CMD ["java", "-jar", "/opt/genomephone/genomephone.jar"]
