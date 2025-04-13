# paddleocr:2.7.5-cpu dockerfile

构建命令:

docker build -t paddleocr:2.7.5-cpu .
docker tag paddleocr:2.7.5-cpu 192.168.124.11:8111/noogel/paddleocr:2.7.5-cpu

发包命令：

docker push 192.168.124.11:8111/noogel/paddleocr:2.7.5-cpu

拉取命令：

docker pull 192.168.124.11:8111/noogel/paddleocr:2.7.5-cpu

测试启动：

docker run --name ppdocr -p 8866:8866 -d 192.168.124.11:8111/noogel/paddleocr:2.7.5-cpu
