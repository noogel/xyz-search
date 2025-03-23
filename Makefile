#!/bin/sh

version = 1.2.2
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
 && docker tag $(project)\:$(version) noogel/$(project)\:latest \
 && docker tag $(project)\:$(version) $(hub)\:$(port)/noogel/$(project)\:$(version) \
 && docker tag $(project)\:$(version) $(hub)\:$(port)/noogel/$(project)\:latest \
 && cd ../

push-local:
	docker push $(hub)\:$(port)/noogel/$(project)\:$(version)
	docker push $(hub)\:$(port)/noogel/$(project)\:latest

push-hub:
	docker push noogel/$(project)\:$(version)
	docker push noogel/$(project)\:latest

git-push-master:
	git add . \
 && git commit -am "auto update" \
 && git push origin master

git-push-dev:
	git add . \
 && git commit -am "auto update" \
 && git push origin dev
