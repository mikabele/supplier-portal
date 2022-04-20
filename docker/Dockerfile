FROM openjdk:17

#tar timezone configs
ENV TIMEZONE=Europe/Minsk
RUN ln -snf /usr/share/zoneinfo/$TIMEZONE /etc/localtime && \
    echo $TIMEZONE > /etc/timezone

#install main utilities
RUN apt-get update && \
    apt-get install -q -y curl && \
    apt-get install -q -y nano

#install scala 2.11
RUN curl -s "https://get.sdkman.io" | bash && \
    sdk install scala 2.13.6

