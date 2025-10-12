FROM ubuntu:latest
LABEL authors="binliu"

ENTRYPOINT ["top", "-b"]
