#!/bin/sh

version = 1.2.1
project = xyz-search
hub = 192.168.124.11
port = 8111

build:
	mvn clean package \
 && cp target/$(project)-*.war docker/ \
 && cd docker \
 && docker build --platform linux/amd64 -t $(project)\:$(version) . \
 && rm $(project)-*.war \
 && docker tag $(project)\:$(version) noogel/$(project)\:$(version) \
 && docker tag $(project)\:$(version) $(hub)\:$(port)/noogel/$(project)\:$(version) \
 && cd ../

push:
	docker push $(hub)\:$(port)/noogel/$(project)\:$(version)

git-push:
	git add . \
 && git commit -am "auto update" \
 && git push origin xyz-feat-spb_3_4
