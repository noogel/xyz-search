#!/bin/sh

version = 1.0.4

build:
	mvn clean package \
 && cd docker/dep1 \
 && docker build -t xyz-search\:$(version) . \
 && rm xyz-search-*.war \
 && docker tag xyz-search\:$(version) 10.168.1.102\:8111/noogel/xyz-search\:$(version) \
 && cd ../../

build-native:
	mvn clean package -Pnative

push:
	docker push 10.168.1.102\:8111/noogel/xyz-search\:$(version)

push-git:
	git add . \
 && git commit -am "auto update" \
 && git push

push-future:
	git add . \
 && git commit -am "auto update" \
 && git push origin future

push-native:
	docker push 10.168.1.102\:8111/noogel/xyz-search\:$(version)-native

