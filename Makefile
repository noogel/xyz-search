#!/bin/sh

version = 1.2.2
project = xyz-search
hub = 192.168.124.11
port = 8111

build:
	mvn clean package \
 && cp target/$(project)-*.war docker/xyz-search/ \
 && cd docker/xyz-search \
 && docker build --platform linux/amd64 -t $(project)\:$(version) . \
 && rm $(project)-*.war \
 && docker tag $(project)\:$(version) noogel/$(project)\:$(version) \
 && docker tag $(project)\:$(version) noogel/$(project)\:latest \
 && docker tag $(project)\:$(version) $(hub)\:$(port)/noogel/$(project)\:$(version) \
 && docker tag $(project)\:$(version) $(hub)\:$(port)/noogel/$(project)\:latest \
 && cd ../../

push:
	docker push $(hub)\:$(port)/noogel/$(project)\:$(version)
	docker push $(hub)\:$(port)/noogel/$(project)\:latest

push-hub:
	docker push noogel/$(project)\:$(version)
	docker push noogel/$(project)\:latest

git-push-master:
	git add . \
 && git commit -am "auto update" \
 && git push origin master

gph:
	git add . \
 && git commit -am "auto update" \
 && git push origin dev

gpl:
	git pull origin dev

grs:
	git checkout master && git fetch origin && git pull && git branch -D dev && git checkout -b dev && git push origin dev -f