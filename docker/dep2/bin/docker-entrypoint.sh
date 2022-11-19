#!/bin/bash
set -e

# Files created by Elasticsearch should always be group writable too
umask 0002

# Allow user specify custom CMD, maybe bin/elasticsearch itself
# for example to directly specify `-E` style parameters for elasticsearch on k8s
# or simply to run /bin/bash to check the image
if [[ "$1" == "eswrapper" || $(basename "$1") == "elasticsearch" ]]; then
  # Rewrite CMD args to remove the explicit command,
  # so that we are backwards compatible with the docs
  # from the previous Elasticsearch versions < 6
  # and configuration option:
  # https://www.elastic.co/guide/en/elasticsearch/reference/5.6/docker.html#_d_override_the_image_8217_s_default_ulink_url_https_docs_docker_com_engine_reference_run_cmd_default_command_or_options_cmd_ulink
  # Without this, user could specify `elasticsearch -E x.y=z` but
  # `bin/elasticsearch -E x.y=z` would not work. In any case,
  # we want to continue through this script, and not exec early.
  set -- "${@:2}"
else
  # Run whatever command the user wanted
  exec "$@"
fi

source /usr/share/elasticsearch/bin/elasticsearch-env-from-file

# Using file log in elasticsearch
cp -f /usr/share/elasticsearch/config/log4j2.file.properties /usr/share/elasticsearch/config/log4j2.properties

# demand process for xyz search
/usr/share/elasticsearch/jdk/bin/java -jar /usr/share/xyz-search/xyz-search.war "$XYZ_JAVA_OPTS" > /usr/share/xyz-search/data/xyz-search.log 2>&1 &

# Signal forwarding and child reaping is handled by `tini`, which is the
# actual entrypoint of the container
exec /usr/share/elasticsearch/bin/elasticsearch "$@"
