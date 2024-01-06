#!/bin/sh

version = 1.0.5
project = xyz-search
hub = nas.noogel.xyz
port = 8111

build:
	mvn clean package \
 && cd docker/dep1 \
 && docker build -t $(project)\:$(version) . \
 && rm $(project)-*.war \
 && docker tag $(project)\:$(version) $(hub)\:$(port)/noogel/$(project)\:$(version) \
 && cd ../../

push:
	docker push $(hub)\:$(port)/noogel/$(project)\:$(version)

git-push:
	git add . \
 && git commit -am "auto update" \
 && git push

git-push-future:
	git add . \
 && git commit -am "auto update" \
 && git push origin future
